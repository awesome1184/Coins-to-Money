"""SHA-512-pinned published binaries for separate integration-test instances only."""
import hashlib, json
from pathlib import Path
from urllib.request import Request, urlopen
for d in json.loads(Path('ci/integration-lock.json').read_text()):
    if not d['url'].startswith('https://cdn.modrinth.com/') or Path(d['filename']).name != d['filename']:
        raise SystemExit('Unexpected dependency URL/path')
    with urlopen(Request(d['url'], headers={'User-Agent':'CoinsToMoney-integration-tests/1.2.3'}), timeout=90) as response:
        data = response.read(64 * 1024 * 1024 + 1)
    if len(data) > 64 * 1024 * 1024 or hashlib.sha512(data).hexdigest() != d['sha512']:
        raise SystemExit('Dependency size/hash mismatch: ' + d['filename'])
    for mode in d['modes']:
        dest = Path('build/integration-mods') / mode
        dest.mkdir(parents=True, exist_ok=True)
        (dest / d['filename']).write_bytes(data)
    print('Verified', d['filename'])
