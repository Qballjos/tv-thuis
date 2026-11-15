#!/usr/bin/env python3
"""Prepare a local upstream candidate, then publish only its explicitly tested commit.

Preparation never writes to a remote. Publication is a separate command so a failed
check cannot accidentally update main. All pushes are ordinary fast-forward pushes.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess

DEFAULT_UPSTREAM = 'https://github.com/simbasjungles/free-my-tv.git'


class SyncError(RuntimeError):
    """A candidate cannot safely be prepared or published."""


def git(repo, *args, check=True):
    result = subprocess.run(['git', '-C', str(repo), *args], text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if check and result.returncode:
        raise SyncError(result.stdout.strip())
    return result


def value(repo, *args):
    return git(repo, *args).stdout.strip()


def save(state_path, state):
    state_path = Path(state_path).resolve()
    state_path.parent.mkdir(parents=True, exist_ok=True)
    temporary = state_path.with_suffix(state_path.suffix + '.tmp')
    temporary.write_text(json.dumps(state, indent=2) + '\n')
    temporary.replace(state_path)
    output = os.environ.get('GITHUB_OUTPUT')
    if output:
        with open(output, 'a') as target:
            for key in ('needed', 'base', 'head', 'branch', 'report'):
                item = state.get(key, '')
                if isinstance(item, bool):
                    item = str(item).lower()
                if '\n' in str(item) or '\r' in str(item):
                    raise SyncError('Invalid newline in workflow output')
                target.write(f'{key}={item}\n')
    return state


def clean(repo):
    if value(repo, 'status', '--porcelain', '--untracked-files=no'):
        raise SyncError('Tracked working tree changes must be saved before upstream sync')
    if git(repo, 'rev-parse', '--verify', 'MERGE_HEAD', check=False).returncode == 0:
        raise SyncError('An existing merge must be resolved before upstream sync')


def fetch_main(repo):
    git(repo, 'fetch', 'origin', 'main:refs/remotes/origin/main')
    return value(repo, 'rev-parse', 'refs/remotes/origin/main')


def prepare(repo, state_path, upstream=DEFAULT_UPSTREAM):
    """Return/save candidate state; abort failed merges and retain a diagnostic report."""
    repo = Path(repo).resolve()
    clean(repo)
    base = fetch_main(repo)
    git(repo, 'fetch', upstream, 'main')
    incoming = value(repo, 'rev-parse', 'FETCH_HEAD')
    state = {'needed': False, 'status': 'noop', 'base': base,
             'head': base, 'branch': '', 'upstream': incoming}
    if git(repo, 'merge-base', '--is-ancestor', incoming, base, check=False).returncode == 0:
        return save(state_path, state)
    branch = 'codex/upstream-' + incoming[:12]
    state.update(branch=branch, status='preparing')
    save(state_path, state)
    if git(repo, 'show-ref', '--verify', '--quiet', 'refs/heads/' + branch, check=False).returncode == 0:
        existing = value(repo, 'rev-parse', branch)
        # Reuse a completed local candidate only when it was built from the same base.
        parents = value(repo, 'rev-list', '--parents', '-n', '1', existing).split()[1:]
        if existing == base:
            git(repo, 'switch', branch)
        elif len(parents) == 2 and parents == [base, incoming]:
            git(repo, 'switch', branch)
            state.update(needed=True, status='prepared', head=existing)
            return save(state_path, state)
        else:
            raise SyncError('Existing local candidate has a different base; refusing to replace it')
    else:
        git(repo, 'switch', '-c', branch, base)
    merged = git(repo, 'merge', '--no-ff', incoming, '-m', 'Merge upstream Free My TV.', check=False)
    if merged.returncode:
        conflicts = value(repo, 'diff', '--name-only', '--diff-filter=U')
        diagnostic = value(repo, 'status', '--short')
        report = Path(state_path).resolve().with_suffix('.conflict.txt')
        report.write_text('Upstream candidate could not merge. Main was not changed.\n'
                          f'Base: {base}\nUpstream: {incoming}\nCandidate: {branch}\n\n'
                          f'Conflicted paths:\n{conflicts or "(merge failed without conflicted paths)"}\n\n'
                          f'{diagnostic}\n\n{merged.stdout}')
        if git(repo, 'rev-parse', '--verify', 'MERGE_HEAD', check=False).returncode == 0:
            git(repo, 'merge', '--abort')
        state.update(status='conflict' if conflicts else 'error', report=str(report))
        save(state_path, state)
        raise SyncError('Upstream merge failed; report: ' + str(report))
    state.update(needed=True, status='prepared', head=value(repo, 'rev-parse', 'HEAD'))
    return save(state_path, state)


def publish(repo, state_path, tested_head):
    """Publish a successfully checked candidate only if origin/main still matches its base."""
    repo = Path(repo).resolve()
    state = json.loads(Path(state_path).read_text())
    if not state.get('needed') or state.get('status') != 'prepared':
        raise SyncError('No prepared candidate to publish')
    clean(repo)
    head = value(repo, 'rev-parse', 'HEAD')
    branch = value(repo, 'branch', '--show-current')
    if head != state['head'] or tested_head != state['head'] or branch != state['branch']:
        raise SyncError('Working branch/HEAD does not match the tested candidate')
    if fetch_main(repo) != state['base']:
        raise SyncError('origin/main changed since preparation; refusing publication')
    if git(repo, 'merge-base', '--is-ancestor', state['base'], head, check=False).returncode:
        raise SyncError('Candidate does not extend the prepared main')
    # Publish both refs atomically: a rejected branch or main leaves both unchanged.
    # No force or force-with-lease is used; server-side fast-forward checks still apply.
    git(repo, 'push', '--atomic', 'origin',
        f'{head}:refs/heads/{branch}', f'{head}:refs/heads/main')
    state.update(status='published')
    return save(state_path, state)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('prepare', 'publish'))
    parser.add_argument('--repo', type=Path, default=Path.cwd())
    parser.add_argument('--state', type=Path, required=True)
    parser.add_argument('--upstream', default=DEFAULT_UPSTREAM)
    parser.add_argument('--tested-head')
    args = parser.parse_args(argv)
    try:
        if args.command == 'prepare':
            state = prepare(args.repo, args.state, args.upstream)
        else:
            if not args.tested_head:
                parser.error('publish requires --tested-head from the successful check')
            state = publish(args.repo, args.state, args.tested_head)
        print(json.dumps(state, indent=2))
        return 0
    except (SyncError, OSError, ValueError) as error:
        print('Upstream sync stopped: ' + str(error))
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
