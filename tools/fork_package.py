#!/usr/bin/env python3
"""Build-time household metadata and Android components; upstream source files remain untouched."""
import argparse
import copy
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', ANDROID[1:-1])


def load(root=ROOT):
    info = json.loads((Path(root) / 'fork/package.json').read_text(encoding='utf-8'))
    version_code(info['version'])
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', info['repository']):
        raise ValueError('Invalid release repository')
    if not re.fullmatch(r'[a-f0-9]{64}', info['certificate']):
        raise ValueError('Invalid release certificate')
    return info


def version_code(version):
    if not re.fullmatch(r'\d+\.\d+\.\d+', version):
        raise ValueError('Expected a three-part version')
    major, minor, patch = map(int, version.split('.'))
    code = major * 1000000 + minor * 10000 + patch
    if minor >= 100 or patch >= 10000 or code > 2100000000:
        raise ValueError('Version exceeds Android allocation')
    return code


def merge_nodes(target, overlay):
    target.attrib.update(overlay.attrib)
    for child in overlay:
        key = child.get(ANDROID + 'name')
        # Containers are singletons even when the base application names its App class
        # and the overlay only adds components. A second application is ignored by Android.
        singleton = target.tag == 'manifest' and child.tag in ('application', 'queries', 'uses-sdk')
        match = next((node for node in target if node.tag == child.tag
                      and (singleton or node.get(ANDROID + 'name') == key)), None)
        if match is None:
            target.append(copy.deepcopy(child))
        else:
            merge_nodes(match, child)


def android_manifest(root=ROOT, info=None):
    root = Path(root)
    info = info or load(root)
    manifest = ET.fromstring((root / 'app/AndroidManifest.xml').read_bytes())
    if manifest.get('package') != 'io.freemytv':
        raise ValueError('Upstream Android package changed: review installed-app compatibility')
    if len(manifest.findall('application')) != 1:
        raise ValueError('Expected exactly one Android application block')
    merge_nodes(manifest, ET.fromstring((root / 'fork/android-manifest.xml').read_bytes()))
    if len(manifest.findall('application')) != 1:
        raise ValueError('Manifest overlay must preserve exactly one Android application block')
    manifest.set(ANDROID+'versionName', info['version'])
    manifest.set(ANDROID+'versionCode', str(version_code(info['version'])))
    for node in manifest.iter():
        if node.get(ANDROID+'label') in ('Free My TV', '@string/app_name'):
            node.set(ANDROID+'label', info['title'])
    return ET.tostring(manifest, encoding='utf-8', xml_declaration=True)


def java_build(info):
    values = {'LATEST': 'https://api.github.com/repos/'+info['repository']+'/releases/latest',
              'RELEASES': 'https://github.com/'+info['repository']+'/releases/download/',
              'RELEASE_CERT': info['certificate']}
    fields = '\n'.join('    static final String '+key+' = '+json.dumps(value)+';' for key, value in values.items())
    return 'package io.freemytv;\n/** Generated from fork/package.json. */\nfinal class ForkBuild {\n'+fields+'\n}\n'


def webos_info(source, info=None):
    info = info or load()
    if source['id'] != 'io.freemytv':
        raise ValueError('Upstream webOS ID changed: review installed-app compatibility')
    return dict(source, version=info['version'], title=info['title'], vendor=info['title'])


def tizen_manifest(source, info=None):
    info = info or load()
    if 'id="FreeMyTVio.FreeMyTV"' not in source:
        raise ValueError('Upstream Tizen ID changed: review installed-app compatibility')
    staged = re.sub(r'(<widget\b[^>]*\bversion=")[^"]+', lambda m: m[1]+info['version'], source, count=1)
    return re.sub(r'(<name>)[^<]*(</name>)', lambda m: m[1]+escape(info['title'])+m[2], staged, count=1)


def stage(output):
    info = load()
    output = Path(output)
    (output/'gen/io/freemytv').mkdir(parents=True, exist_ok=True)
    (output/'res/values').mkdir(parents=True, exist_ok=True)
    (output/'AndroidManifest.xml').write_bytes(android_manifest(info=info))
    (output/'gen/io/freemytv/ForkBuild.java').write_text(java_build(info), encoding='utf-8')
    resources = ET.Element('resources')
    ET.SubElement(resources, 'string', {'name':'app_name'}).text = info['title']
    (output/'res/values/fork.xml').write_bytes(ET.tostring(resources, encoding='utf-8'))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', help='build staging directory (usually out)')
    stage(parser.parse_args().output)
