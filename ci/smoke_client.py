"""Exercise actual transformed sidebar rows and 120 settings/currency frames in a real Fabric client.
Uses only synthetic fixtures; no Minecraft account or live Hypixel connection is required.
"""
import os
from pathlib import Path
import signal
import subprocess
import time

compat = os.environ.get('CTM_CUSTOMSCOREBOARD') == '1'
log = Path('build/ci-customscoreboard.log' if compat else 'build/ci-client.log')
log.parent.mkdir(exist_ok=True)
with log.open('w') as output:
    process = subprocess.Popen(['xvfb-run', '-a', 'gradle', 'runSmokeClient', '--no-daemon'] + (['-PcustomScoreboardTest'] if compat else []),
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
markers = ('CTM_ROW_TESTS_PASS', 'CTM_PURSE_DIAGNOSTIC_PASS', 'CTM_RENDER_TESTS_PASS')
markers += ('CTM_RESOURCE_COSTS_PASS', 'CTM_CUSTOMSCOREBOARD_PASS' if compat else 'CTM_OPTIONAL_COMPAT_ABSENT_PASS')
if process.returncode != 0 or any(marker not in text for marker in markers):
    raise SystemExit('Actual sidebar/settings rendering regression failed')
print('PASS: transformed vanilla rows, widths, saved settings and 120 real settings/currency/tooltip/blur/sidebar frames')
