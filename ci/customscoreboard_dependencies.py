"""Pinned published mods for CI only; never bundled into the release JAR."""
import hashlib
import json
from pathlib import Path
from urllib.request import Request, urlopen

out = Path('build/compat-mods')
out.mkdir(parents=True, exist_ok=True)
for dependency in json.loads(Path('ci/customscoreboard-lock.json').read_text()):
    url, name = dependency['url'], dependency['filename']
    if not url.startswith('https://cdn.modrinth.com/') or Path(name).name != name:
        raise SystemExit('Unexpected dependency path')
    request = Request(url, headers={'User-Agent': 'CoinsToMoney-compatibility-CI/1.2.2'})
    with urlopen(request, timeout=60) as response:
        data = response.read(64 * 1024 * 1024 + 1)
    if len(data) > 64 * 1024 * 1024 or hashlib.sha512(data).hexdigest() != dependency['sha512']:
        raise SystemExit('Dependency checksum/size mismatch: ' + name)
    (out / name).write_bytes(data)
    print('Verified', name)
