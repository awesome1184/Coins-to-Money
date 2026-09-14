"""Exercise actual transformed sidebar rows and 120 settings/currency frames in a real Fabric client.
Uses only synthetic fixtures; no Minecraft account or live Hypixel connection is required.
"""
import os
from pathlib import Path
import signal
import subprocess
import time

log = Path('build/ci-client.log')
log.parent.mkdir(exist_ok=True)
with log.open('w') as output:
    process = subprocess.Popen(['xvfb-run', '-a', 'gradle', 'runSmokeClient', '--no-daemon'],
                               stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
    try:
        process.wait(timeout=360)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
        raise SystemExit('Client render tests timed out; see build/ci-client.log')
text = log.read_text(errors='replace')
print(text[-14000:])
markers = ('CTM_ROW_TESTS_PASS', 'CTM_RENDER_TESTS_PASS')
if process.returncode != 0 or any(marker not in text for marker in markers):
    raise SystemExit('Actual sidebar/settings rendering regression failed')
print('PASS: transformed vanilla rows, widths, saved settings and 120 real settings/currency/tooltip/blur/sidebar frames')
