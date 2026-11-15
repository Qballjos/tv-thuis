"""Regression checks for fork text layering; no translation API calls."""
import contextlib
import io
import json
import pathlib
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / 'tools'))
import translate


class OverlayTests(unittest.TestCase):
    def test_split_preserves_fork_translations_and_follows_upstream_updates(self):
        snapshot = json.loads(pathlib.Path(__file__).with_name('translation-overlay-snapshot.json').read_text())
        for lang, fork_values in snapshot.items():
            effective = translate.load(lang)
            for key, expected in fork_values.items():
                self.assertEqual(effective[key], expected, (lang, key))
            base = translate.read_catalogue(translate.I18N, lang)
            for key, expected in base.items():
                if key not in fork_values:
                    self.assertEqual(effective[key], expected, (lang, key))

    def test_upstream_edit_and_new_language_survive_fork_fallback(self):
        with tempfile.TemporaryDirectory() as directory:
            base = pathlib.Path(directory) / 'i18n'
            fork = pathlib.Path(directory) / 'fork-i18n'
            base.mkdir()
            fork.mkdir()
            for folder, lang, data in [(base, 'en', {'Live TV': 'Live TV'}),
                                       (base, 'nl', {'Live TV': 'Updated upstream Dutch'}),
                                       (base, 'xx', {'Live TV': 'New upstream language'}),
                                       (fork, 'en', {'Profiles': 'Profiles'}),
                                       (fork, 'nl', {'Profiles': 'Profielen'})]:
                (folder / (lang + '.json')).write_text(json.dumps(data))
            with patch.object(translate, 'I18N', str(base)), patch.object(translate, 'FORK_I18N', str(fork)):
                self.assertEqual(translate.load('nl'), {'Live TV': 'Updated upstream Dutch', 'Profiles': 'Profielen'})
                self.assertEqual(translate.load('xx'), {'Live TV': 'New upstream language', 'Profiles': 'Profiles'})
                self.assertIn('xx', translate.languages())

    def test_prune_keeps_upstream_and_hand_text_and_removes_only_obsolete_overlay(self):
        with tempfile.TemporaryDirectory() as directory:
            base = pathlib.Path(directory) / 'i18n'
            fork = pathlib.Path(directory) / 'fork-i18n'
            base.mkdir()
            fork.mkdir()
            for folder, lang, data in [(base, 'en', {'Live TV': 'Live TV', 'Unused': 'Unused'}),
                                       (base, 'pt', {'Live TV': 'TV original', 'Unused': 'Antigo'}),
                                       (fork, 'en', {'Profiles': 'Profiles'}),
                                       (fork, 'pt', {'Live TV': 'TV manual', 'Unused': 'Old override', 'Profiles': 'Perfis'})]:
                (folder / (lang + '.json')).write_text(json.dumps(data))
            original = {p.name: p.read_bytes() for p in base.glob('*.json')}
            with patch.object(translate, 'I18N', str(base)), patch.object(translate, 'FORK_I18N', str(fork)), \
                    patch.object(translate, 'CACHE', str(pathlib.Path(directory) / 'cache.json')), \
                    patch.object(translate, 'extract', return_value={'Live TV': 'Live TV', 'Profiles': 'Profiles'}), \
                    patch.object(sys, 'argv', ['translate.py', '--prune', '--fill']), \
                    patch.dict('os.environ', {'ANTHROPIC_API_KEY': ''}), contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(translate.main(), 0)
                self.assertEqual(translate.check(translate.extract()), 0)
            self.assertEqual({p.name: p.read_bytes() for p in base.glob('*.json')}, original)
            self.assertEqual(json.loads((fork / 'pt.json').read_text()), {'Live TV': 'TV manual', 'Profiles': 'Perfis'})

    def test_language_without_fork_translation_gets_english_fork_text(self):
        with tempfile.TemporaryDirectory() as directory:
            base = pathlib.Path(directory) / 'i18n'
            fork = pathlib.Path(directory) / 'fork-i18n'
            base.mkdir()
            fork.mkdir()
            for folder, lang, data in [(base, 'en', {'Live TV': 'Live TV'}),
                                       (base, 'nl', {'Live TV': 'Live tv'}),
                                       (fork, 'en', {'Profiles': 'Profiles'}),
                                       (fork, 'nl', {'Live TV': 'Televisie'})]:
                (folder / (lang + '.json')).write_text(json.dumps(data))
            with patch.object(translate, 'I18N', str(base)), patch.object(translate, 'FORK_I18N', str(fork), create=True):
                self.assertEqual(translate.load('nl'), {'Live TV': 'Televisie', 'Profiles': 'Profiles'})
                translate.save('nl', {'Live TV': 'Televisie', 'Profiles': 'Profielen'})
                self.assertEqual(json.loads((base / 'nl.json').read_text()), {'Live TV': 'Live tv'})
                self.assertEqual(json.loads((fork / 'nl.json').read_text()), {'Live TV': 'Televisie', 'Profiles': 'Profielen'})


if __name__ == '__main__':
    unittest.main()
