import pathlib
import re
import subprocess
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).with_name('release-version.py')

class ReleaseVersionTest(unittest.TestCase):
    def test_successive_releases_after_source_version_bump_increase_code(self):
        source = '<manifest android:versionCode="1030010" android:versionName="1.3.10">'
        numbers = []
        with tempfile.TemporaryDirectory() as folder:
            manifest = pathlib.Path(folder) / 'AndroidManifest.xml'
            for latest in ['v1.3.9', 'v1.3.10', 'v1.3.11']:
                manifest.write_text(source)
                result = subprocess.run(['python3', str(SCRIPT), str(manifest), latest], capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)
                numbers.append(int(re.search(r'versionCode="(\d+)"', manifest.read_text())[1]))
        self.assertLess(numbers[0], numbers[1])
        self.assertLess(numbers[1], numbers[2])

    def test_increments_published_version_and_code(self):
        with tempfile.TemporaryDirectory() as folder:
            manifest = pathlib.Path(folder) / 'AndroidManifest.xml'
            manifest.write_text('<manifest android:versionCode="11" android:versionName="1.3.7">')
            result = subprocess.run(['python3', str(SCRIPT), str(manifest), 'v1.3.8'], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(result.stdout.strip(), '1.3.9')
            self.assertIn('versionCode="1030009"', manifest.read_text())
            self.assertIn('versionName="1.3.9"', manifest.read_text())

    def test_preserves_higher_source_version(self):
        with tempfile.TemporaryDirectory() as folder:
            manifest = pathlib.Path(folder) / 'AndroidManifest.xml'
            manifest.write_text('<manifest android:versionCode="20" android:versionName="1.4.0">')
            result = subprocess.run(['python3', str(SCRIPT), str(manifest), 'v1.3.8'], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(result.stdout.strip(), '1.4.0')
            self.assertIn('versionCode="1040000"', manifest.read_text())

if __name__ == '__main__':
    unittest.main()
