"""Boot the real Fabric client under a virtual display and require our ready marker.
This verifies entrypoints and mixin application, not a live Hypixel play session.
"""
import os
from pathlib import Path
import signal
import subprocess
import time

log = Path('build/ci-client.log')
log.parent.mkdir(exist_ok=True)
with log.open('w') as output:
    process = subprocess.Popen(['xvfb-run', '-a', 'gradle', 'runClient', '--no-daemon'], stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
    passed = False
    try:
        deadline = time.monotonic() + 300
        while time.monotonic() < deadline:
            if 'CTM_CLIENT_READY' in log.read_text(errors='replace'):
                passed = True
                break
            if process.poll() is not None:
                break
            time.sleep(1)
    finally:
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
print(log.read_text(errors='replace')[-16000:])
if not passed:
    raise SystemExit('Real Fabric client did not reach the Coins to Money ready marker')
print('PASS: real client entrypoints and team-name mixin loaded')
