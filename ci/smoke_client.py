"""Actual Fabric rendering checks, with and without the published optional mods."""
import os
import sys
from pathlib import Path
import signal
import subprocess

compat = '--customscoreboard' in sys.argv
log = Path('build/ci-customscoreboard.log' if compat else 'build/ci-client.log')
log.parent.mkdir(exist_ok=True)
command = ['xvfb-run', '-a', 'gradle', 'runSmokeClient', '--no-daemon']
if compat:
    command.append('-PcustomScoreboardTest')
with log.open('w') as output:
    process = subprocess.Popen(command, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
    try:
        process.wait(timeout=360)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
        raise SystemExit('Client render tests timed out; see ' + str(log))
text = log.read_text(errors='replace')
print(text[-14000:])
markers = ('CTM_ROW_TESTS_PASS', 'CTM_PURSE_DIAGNOSTIC_PASS', 'CTM_RENDER_TESTS_PASS',
           'CTM_CUSTOMSCOREBOARD_PASS' if compat else 'CTM_COMPAT_ABSENT_PASS')
if process.returncode != 0 or any(marker not in text for marker in markers):
    raise SystemExit('Actual sidebar/settings rendering regression failed')
print('PASS: actual client rendering tests; CustomScoreboard=' + str(compat))
