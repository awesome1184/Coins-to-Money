"""Publish the exact tested JAR selected in .github/release.json, without rebuilding."""
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def gh(*args):
    return subprocess.check_output(['gh', *args])


def api(path):
    return json.loads(gh('api', path))


def main():
    manifest = json.loads(Path('.github/release.json').read_text(encoding='utf-8'))
    repo = os.environ['GITHUB_REPOSITORY']
    version, commit = manifest['version'], manifest['commit']
    require(re.fullmatch(r'\d+\.\d+\.\d+', version), 'Invalid release version')
    require(re.fullmatch(r'[0-9a-f]{40}', commit), 'Expected an immutable commit SHA')
    for key in ['jar_sha256', 'artifact_sha256']:
        require(re.fullmatch(r'[0-9a-f]{64}', manifest[key]), 'Invalid SHA-256')
    run_id, artifact_id = int(manifest['run_id']), int(manifest['artifact_id'])
    endpoint = f'repos/{repo}'
    run = api(f'{endpoint}/actions/runs/{run_id}')
    require(run['head_sha'] == commit and run['event'] == 'push'
            and run['path'] == '.github/workflows/build.yml'
            and run['status'] == 'completed' and run['conclusion'] == 'success',
            'Release requires a successful build.yml push run of the selected commit')
    compare = api(f'{endpoint}/compare/{commit}...main')
    require(compare['status'] in ['ahead', 'identical'], 'Selected commit is not on main')
    artifact = api(f'{endpoint}/actions/artifacts/{artifact_id}')
    require(not artifact['expired'] and artifact['workflow_run']['id'] == run_id
            and artifact['workflow_run']['head_sha'] == commit
            and artifact['name'] == f'coins-to-money-{commit}', 'Artifact provenance mismatch')
    require(artifact['digest'] == 'sha256:' + manifest['artifact_sha256'], 'Artifact digest mismatch')

    archive = gh('api', f'{endpoint}/actions/artifacts/{artifact_id}/zip')
    require(hashlib.sha256(archive).hexdigest() == manifest['artifact_sha256'], 'Downloaded archive mismatch')
    filename = f'coins-to-money-{version}.jar'
    with zipfile.ZipFile(io.BytesIO(archive)) as package:
        jar = package.read(f'libs/{filename}')
        require(hashlib.sha256(jar).hexdigest() == manifest['jar_sha256'], 'JAR checksum mismatch')
        with zipfile.ZipFile(io.BytesIO(jar)) as mod:
            metadata = json.loads(mod.read('fabric.mod.json'))
            require(metadata['id'] == 'coins-to-money' and metadata['version'] == version, 'JAR version mismatch')
            require(not any('/smoke/' in name for name in mod.namelist()), 'Test fixtures in JAR')
        reports = [ET.fromstring(package.read(name)) for name in package.namelist()
                   if name.startswith('test-results/test/TEST-') and name.endswith('.xml')]
        require(reports and sum(int(report.attrib['tests']) for report in reports) > 0, 'Missing test results')
        require(all(int(report.attrib.get(key, 0)) == 0 for report in reports
                    for key in ['failures', 'errors', 'skipped']), 'Tests must all pass')
        require('CTM_RENDER_TESTS_PASS:' in package.read('ci-client.log').decode('utf-8'), 'Missing client rendering check')

    output = Path('release-dist')
    output.mkdir(exist_ok=True)
    (output / filename).write_bytes(jar)
    checksums = f"{manifest['jar_sha256']}  {filename}\n".encode()
    (output / 'SHA256SUMS').write_bytes(checksums)
    (output / 'notes.md').write_text(manifest['notes'] + f'\n\nSource commit: `{commit}`.\n', encoding='utf-8')
    tag = 'v' + version
    # Never move a pre-existing tag, including an annotated tag, to a new commit.
    refs = api(f'{endpoint}/git/matching-refs/tags/{tag}')
    matches = [ref for ref in refs if ref['ref'] == f'refs/tags/{tag}']
    if matches:
        obj = matches[0]['object']
        for _ in range(10):
            if obj['type'] != 'tag':
                break
            obj = api(f"{endpoint}/git/tags/{obj['sha']}")['object']
        require(obj['type'] == 'commit' and obj['sha'] == commit, 'Existing release tag points elsewhere')

    pages = json.loads(gh('api', '--paginate', '--slurp', f'{endpoint}/releases?per_page=100'))
    release = next((release for page in pages for release in page if release['tag_name'] == tag), None)
    if release is None:
        # Use the returned release ID; the releases listing can lag creation.
        release = json.loads(gh('api', '--method', 'POST', f'{endpoint}/releases',
                                '-f', f'tag_name={tag}', '-f', f'target_commitish={commit}',
                                '-f', f'name=Coins to Money {version}', '-F', 'draft=true',
                                '-f', 'body=' + (output / 'notes.md').read_text(encoding='utf-8')))
    if not matches:
        require(release['draft'] and release['target_commitish'] == commit, 'Draft target mismatch')

    for asset_name, data in [(filename, jar), ('SHA256SUMS', checksums)]:
        asset = next((asset for asset in release['assets'] if asset['name'] == asset_name), None)
        if asset:
            require(asset.get('digest') == 'sha256:' + hashlib.sha256(data).hexdigest(),
                    'Existing release asset differs; refusing to overwrite ' + asset_name)
        else:
            require(release['draft'], 'Published release lacks expected asset; refusing to modify it')
            content_type = 'application/java-archive' if asset_name.endswith('.jar') else 'text/plain'
            gh('api', '--method', 'POST',
               f"https://uploads.github.com/repos/{repo}/releases/{release['id']}/assets?name={asset_name}",
               '-H', 'Content-Type: ' + content_type, '--input', str(output / asset_name))
    if release['draft']:
        gh('api', '--method', 'PATCH', f"{endpoint}/releases/{release['id']}",
           '-F', 'draft=false', '-f', 'make_latest=true')
    print(f'https://github.com/{repo}/releases/tag/{tag}')
    print('Published verified JAR SHA-256: ' + manifest['jar_sha256'])


if __name__ == '__main__':
    main()
