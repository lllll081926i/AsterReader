"""从已解析的运行库与上游原文收集离线许可；构建过程只读取已提交文件。"""
import hashlib
import io
import json
import os
from pathlib import Path
import re
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches/modules-2/files-2.1'
DEST = ROOT / 'app/src/main/licenses/runtime'
DEST.mkdir(parents=True, exist_ok=True)


def fetch(url):
    with urllib.request.urlopen(url, timeout=45) as response:
        return response.read().decode('utf-8-sig')


def save(text):
    text = text.strip() + '\n'
    name = hashlib.sha256(text.encode()).hexdigest()[:24] + '.txt'
    (DEST / name).write_text(text, encoding='utf-8')
    return name


def embedded(data):
    result = []
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for item in archive.infolist():
            name = item.filename.rsplit('/', 1)[-1]
            if re.match(r'(?i)^(license|notice|copying|copyright)([._-]|$)', name) and 0 < item.file_size < 512_000:
                text = archive.read(item).decode('utf-8', errors='replace')
                result.append(item.filename + '\n\n' + text)
            elif item.filename == 'classes.jar':
                result.extend(embedded(archive.read(item)))
    return result


apache = save(fetch('https://www.apache.org/licenses/LICENSE-2.0.txt'))
special = {
    'com.github.junrar': save(fetch('https://raw.githubusercontent.com/junrar/junrar/v8.1.2/LICENSE')),
    'org.slf4j': save(fetch('https://raw.githubusercontent.com/qos-ch/slf4j/v_2.0.17/LICENSE.txt')),
    'org.bouncycastle': save(fetch('https://raw.githubusercontent.com/bcgit/bc-java/r1rv86/LICENSE.html')),
    'org.tukaani': save(fetch('https://raw.githubusercontent.com/tukaani-project/xz-java/v1.9/COPYING')),
}
skia = save(fetch('https://raw.githubusercontent.com/google/skia/main/LICENSE'))
entries = {}
for variant in ('debug', 'release'):
    for entry in json.loads((ROOT / f'app/build/generated/notices/{variant}/open_source_notices.json').read_text('utf-8')):
        entries[entry['id']] = entry
index = {}
for entry in entries.values():
    coordinate = entry['id']
    if coordinate.startswith('font:'):
        continue
    group, artifact, version = coordinate.split(':')
    license_file = special.get(group)
    if license_file is None:
        if 'apache' not in entry['license'].lower():
            raise RuntimeError('需要核对许可：' + coordinate)
        license_file = apache
    notices = [license_file]
    if group.startswith('androidx.'):
        notices.append(save('Copyright The Android Open Source Project\nLicensed under the Apache License, Version 2.0.'))
    directory = CACHE / group / artifact / version
    for file in sorted(directory.glob('*/*')):
        if file.suffix in ('.jar', '.aar') and not file.name.endswith(('-sources.jar', '-javadoc.jar')):
            notices.extend(save(text) for text in embedded(file.read_bytes()))
    if coordinate.startswith('androidx.graphics:graphics-path:'):
        notices.append(skia)
    index[coordinate] = sorted(set(notices))
(DEST / 'index.json').write_text(json.dumps(index, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('已收集', len(index), '项运行库许可；目录：', DEST)
