"""Exercise upstream candidate preparation/publication using only disposable local repositories."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import shlex
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('upstream_sync', Path(__file__).resolve().parents[1] / 'tools/upstream_sync.py')
sync = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(sync)


def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *args], text=True, stderr=subprocess.STDOUT).strip()


class UpstreamSyncTest(unittest.TestCase):
    def setUp(self):
        environment = patch.dict(os.environ, {'GITHUB_OUTPUT': ''})
        environment.start()
        self.addCleanup(environment.stop)
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.origin = self.root / 'origin.git'
        self.upstream = self.root / 'upstream.git'
        self.seed = self.root / 'seed'
        subprocess.run(['git', 'init', '--bare', '--initial-branch=main', str(self.origin)], check=True, capture_output=True)
        subprocess.run(['git', 'clone', str(self.origin), str(self.seed)], check=True, capture_output=True)
        self.configure(self.seed)
        self.commit(self.seed, 'shared.txt', 'base\n')
        git(self.seed, 'push', 'origin', 'main')
        subprocess.run(['git', 'clone', '--bare', str(self.origin), str(self.upstream)], check=True, capture_output=True)
        self.repo = self.root / 'candidate'
        subprocess.run(['git', 'clone', str(self.origin), str(self.repo)], check=True, capture_output=True)
        self.configure(self.repo)
        self.writer = self.root / 'writer'
        subprocess.run(['git', 'clone', str(self.upstream), str(self.writer)], check=True, capture_output=True)
        self.configure(self.writer)
        self.state = self.root / 'state.json'
        self.base = git(self.origin, 'rev-parse', 'main')

    def configure(self, repo):
        git(repo, 'config', 'user.name', 'Fixture')
        git(repo, 'config', 'user.email', 'fixture@example.invalid')

    def commit(self, repo, filename, content):
        (repo / filename).write_text(content)
        git(repo, 'add', filename)
        git(repo, 'commit', '-m', 'Fixture update')

    def upstream_change(self, filename='upstream.txt', content='new upstream\n'):
        self.commit(self.writer, filename, content)
        git(self.writer, 'push', 'origin', 'main')

    def prepare(self):
        return sync.prepare(self.repo, self.state, str(self.upstream))

    def test_already_integrated_is_noop(self):
        state = self.prepare()
        self.assertFalse(state['needed'])
        self.assertEqual(git(self.repo, 'branch', '--show-current'), 'main')
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), self.base)

    def test_clean_prepare_keeps_main_and_remote_untouched_until_publish(self):
        self.upstream_change()
        state = self.prepare()
        self.assertTrue(state['needed'])
        self.assertEqual(state['base'], self.base)
        self.assertEqual(git(self.repo, 'rev-parse', 'main'), self.base)
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), self.base)
        self.assertEqual(git(self.origin, 'for-each-ref', '--format=%(refname)', 'refs/heads/codex'), '')
        self.assertTrue((self.repo / 'upstream.txt').is_file())
        sync.publish(self.repo, self.state, state['head'])
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), state['head'])
        self.assertEqual(git(self.origin, 'rev-parse', state['branch']), state['head'])

    def test_conflict_aborts_and_keeps_main_with_readable_report(self):
        self.commit(self.seed, 'shared.txt', 'fork\n')
        git(self.seed, 'push', 'origin', 'main')
        base = git(self.origin, 'rev-parse', 'main')
        self.upstream_change('shared.txt', 'upstream\n')
        with self.assertRaises(sync.SyncError):
            self.prepare()
        state = json.loads(self.state.read_text())
        self.assertFalse(state['needed'])
        self.assertEqual(state['status'], 'conflict')
        self.assertIn('shared.txt', Path(state['report']).read_text())
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), base)
        self.assertEqual(git(self.repo, 'ls-files', '-u'), '')
        self.assertFalse((self.repo / '.git/MERGE_HEAD').exists())

    def test_failed_check_caller_does_not_publish(self):
        self.upstream_change()
        self.prepare()
        check = subprocess.run(['python3', '-c', 'raise SystemExit(1)'], capture_output=True)
        self.assertNotEqual(check.returncode, 0)
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), self.base)
        self.assertEqual(git(self.origin, 'for-each-ref', '--format=%(refname)', 'refs/heads/codex'), '')

    def test_main_advance_refuses_even_a_fast_forward(self):
        self.upstream_change()
        state = self.prepare()
        self.commit(self.seed, 'other.txt', 'someone else\n')
        git(self.seed, 'push', 'origin', 'main')
        advanced = git(self.origin, 'rev-parse', 'main')
        with self.assertRaisesRegex(sync.SyncError, 'main changed'):
            sync.publish(self.repo, self.state, state['head'])
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), advanced)
        self.assertEqual(git(self.origin, 'for-each-ref', '--format=%(refname)', 'refs/heads/codex'), '')

    def test_push_time_main_race_rejects_both_refs_atomically(self):
        self.upstream_change()
        state = self.prepare()
        self.commit(self.seed, 'racing.txt', 'race winner\n')
        raced = git(self.seed, 'rev-parse', 'HEAD')
        git(self.seed, 'push', 'origin', 'HEAD:refs/heads/race-object')
        # pre-push runs after remote refs were advertised; change main at that exact boundary.
        hook = self.repo / '.git/hooks/pre-push'
        hook.write_text('#!/bin/sh\nexec git -C ' + shlex.quote(str(self.origin))
                        + ' update-ref refs/heads/main ' + shlex.quote(raced) + '\n')
        hook.chmod(0o755)
        with self.assertRaises(sync.SyncError):
            sync.publish(self.repo, self.state, state['head'])
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), raced)
        self.assertEqual(git(self.origin, 'for-each-ref', '--format=%(refname)', 'refs/heads/codex'), '')

    def test_prepared_candidate_is_reused_without_a_second_branch(self):
        self.upstream_change()
        first = self.prepare()
        second = self.prepare()
        self.assertEqual(first['branch'], second['branch'])
        self.assertEqual(first['head'], second['head'])
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), self.base)

    def test_changed_candidate_cannot_publish_an_untested_commit(self):
        self.upstream_change()
        state = self.prepare()
        self.commit(self.repo, 'untested.txt', 'not checked\n')
        with self.assertRaisesRegex(sync.SyncError, 'tested candidate'):
            sync.publish(self.repo, self.state, state['head'])
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), self.base)

    def test_wrong_tested_head_refuses_publication(self):
        self.upstream_change()
        self.prepare()
        with self.assertRaisesRegex(sync.SyncError, 'tested candidate'):
            sync.publish(self.repo, self.state, self.base)
        self.assertEqual(git(self.origin, 'rev-parse', 'main'), self.base)


if __name__ == '__main__':
    unittest.main()
