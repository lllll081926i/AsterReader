"""校验所有 ABI、签名、版本和离线许可，并生成同一批次的分发文件与哈希。"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--delivery', default='build/release-delivery')
parser.add_argument('--expected-certificate', required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
delivery = root / args.delivery
delivery.mkdir(parents=True, exist_ok=True)
sdk = Path(os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', '')))
tools = sdk / 'build-tools/37.0.0'
suffix = '.bat' if os.name == 'nt' else ''


def run(command):
    return subprocess.check_output([str(value) for value in command], text=True, stderr=subprocess.STDOUT)


def elf_alignment(data):
    assert data[:4] == b'\x7fELF'
    endian = '<' if data[5] == 1 else '>'
    wide = data[4] == 2
    offset = struct.unpack_from(endian + ('Q' if wide else 'I'), data, 32 if wide else 28)[0]
    size, count = struct.unpack_from(endian + 'HH', data, 54 if wide else 42)
    for i in range(count):
        header = struct.unpack_from(endian + ('IIQQQQQQ' if wide else 'IIIIIIII'), data, offset + size * i)
        if header[0] == 1:
            file_offset, address = (header[2], header[3]) if wide else (header[1], header[2])
            assert header[-1] >= 16384 and (file_offset - address) % 16384 == 0, 'ELF 未满足 16 KB LOAD 对齐'


expected = args.expected_certificate.replace(':', '').lower()
assert re.fullmatch('[a-f0-9]{64}', expected), '预期证书无效'
metadata_path = root / 'app/build/outputs/apk/release/output-metadata.json'
metadata = json.loads(metadata_path.read_text('utf-8'))
elements = metadata['elements']
abis = {'universal', 'arm64-v8a', 'armeabi-v7a'}
assert len(elements) == len(abis), '缺少 APK 变体'
version = elements[0]['versionName']
gradle = (root / 'app/build.gradle.kts').read_text('utf-8')
expected_version = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)[1]
expected_code = int(re.search(r'versionCode\s*=\s*(\d+)', gradle)[1])
assert all(item['versionName'] == expected_version and item['versionCode'] == expected_code for item in elements)
found = set()
notices = None
files = []
for item in elements:
    abi_filters = [entry['value'] for entry in item['filters'] if entry['filterType'] == 'ABI']
    abi = abi_filters[0] if abi_filters else 'universal'
    assert abi in abis and abi not in found
    found.add(abi)
    source = metadata_path.parent / item['outputFile']
    certificate = run([tools / ('apksigner' + suffix), 'verify', '--verbose', '--print-certs', source])
    actual = re.search(r'certificate SHA-256 digest:\s*([0-9a-fA-F]{64})', certificate)
    assert 'Number of signers: 1' in certificate and actual and actual[1].lower() == expected, 'APK 证书不符'
    run([tools / ('zipalign.exe' if os.name == 'nt' else 'zipalign'), '-c', '-P', '16', '4', source])
    badging = run([tools / ('aapt2.exe' if os.name == 'nt' else 'aapt2'), 'dump', 'badging', source])
    assert "name='com.asterreader.app'" in badging and f"versionName='{expected_version}'" in badging
    assert re.search(r"(?:minSdkVersion|sdkVersion):'31'", badging) and "targetSdkVersion:'37'" in badging
    assert 'application-debuggable' not in badging and 'testOnly' not in badging
    with zipfile.ZipFile(source) as archive:
        native = [entry for entry in archive.namelist() if entry.startswith('lib/') and entry.endswith('.so')]
        actual_abis = {entry.split('/')[1] for entry in native}
        assert actual_abis == (abis - {'universal'} if abi == 'universal' else {abi}), 'APK ABI 不符'
        for entry in native:
            if entry.split('/')[1] in {'arm64-v8a', 'x86_64'}:
                elf_alignment(archive.read(entry))
        bundled = json.loads(archive.read('assets/open_source_notices.json'))
        assert bundled and all(entry.get('notice', '').strip() for entry in bundled), '离线许可不完整'
        if notices is None:
            notices = bundled
        else:
            assert notices == bundled
    destination = delivery / f'AsterReader-{version}-{abi}.apk'
    shutil.copy2(source, destination)
    files.append(destination)
assert found == abis
report = root / 'build/reports/release-verification.json'
report.parent.mkdir(parents=True, exist_ok=True)
report.write_text(json.dumps([
    {'file': file.name, 'version': version, 'versionCode': expected_code,
     'certificate': expected, 'sha256': hashlib.sha256(file.read_bytes()).hexdigest()}
    for file in sorted(files)
], ensure_ascii=False, indent=2), encoding='utf-8')
print('校验成功：3 个 APK；证书、版本、ABI、16 KB 对齐与离线许可均通过。')
