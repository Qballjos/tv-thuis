import importlib.util
import json
import pathlib
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location('fork_package', ROOT / 'tools/fork_package.py')

class ForkPackageTest(unittest.TestCase):
    def module(self):
        module = importlib.util.module_from_spec(SPEC)
        SPEC.loader.exec_module(module)
        return module

    def test_android_preserves_upstream_components_and_adds_fork_components(self):
        m = self.module()
        with tempfile.TemporaryDirectory() as folder:
            root = pathlib.Path(folder)
            (root / 'app').mkdir(); (root / 'fork').mkdir()
            source = '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="io.freemytv" android:versionName="9.0" android:versionCode="9"><application android:label="Free My TV"><activity android:name=".NewUpstream" android:exported="false"/></application></manifest>'
            (root / 'app/AndroidManifest.xml').write_text(source)
            (root / 'fork/android-manifest.xml').write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android"><queries><package android:name="player"/></queries><application><activity android:name=".ProfileManagerActivity" android:exported="false"/></application></manifest>')
            info = {'version':'1.3.13','title':'TV Thuis','repository':'Qballjos/tv-thuis','certificate':'94ea93a97b298f691f232eab656ec096d2d2701b4790b7e0308912632fd45138'}
            staged = m.android_manifest(root, info)
            tree = ET.fromstring(staged)
            self.assertEqual(tree.get(m.ANDROID + 'versionName'), '1.3.13')
            self.assertEqual(tree.get(m.ANDROID + 'versionCode'), '1030013')
            self.assertEqual([n.get(m.ANDROID+'name') for n in tree.findall('application/activity')], ['.NewUpstream','.ProfileManagerActivity'])
            self.assertEqual(tree.find('queries/package').get(m.ANDROID+'name'), 'player')
            self.assertEqual((root/'app/AndroidManifest.xml').read_text(), source)

    def test_webos_overrides_only_fork_identity_and_version(self):
        m = self.module()
        source = {'id':'io.freemytv','version':'9.0.0','vendor':'Upstream','title':'Free My TV','resolution':'1280x720','newUpstreamField':True}
        info = {'version':'1.3.13','title':'TV Thuis'}
        result = m.webos_info(source, info)
        self.assertEqual(result['version'], '1.3.13')
        self.assertEqual(result['title'], 'TV Thuis')
        self.assertEqual(result['resolution'], '1280x720')
        self.assertTrue(result['newUpstreamField'])
        self.assertEqual(source['version'], '9.0.0')

    def test_update_identity_is_fork_owned_and_unchanged(self):
        m = self.module()
        generated = m.java_build(m.load())
        self.assertIn('https://api.github.com/repos/Qballjos/tv-thuis/releases/latest', generated)
        self.assertIn('94ea93a97b298f691f232eab656ec096d2d2701b4790b7e0308912632fd45138', generated)

if __name__ == '__main__': unittest.main()
