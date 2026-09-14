"""Exercise enabled sidebar/tooltip hooks and render the real settings screen.
Run inside xvfb-run. A startup log message alone is not a passing test.
"""
from pathlib import Path
import os
import re
import signal
import subprocess
import time

log = Path('build/ci-client.log')
log.parent.mkdir(exist_ok=True)
markers = {
    'CTM_SIDEBAR_RENDERED': 'build/sidebar-tooltip.png',
    'CTM_CONFIG_RENDERED': 'build/settings-screen.png',
}
captured = set()
with log.open('w') as output:
    process = subprocess.Popen(['gradle', 'runSmokeClient', '--no-daemon'], stdout=output,
                               stderr=subprocess.STDOUT, start_new_session=True)
    try:
        deadline = time.monotonic() + 360
        while process.poll() is None and time.monotonic() < deadline:
            text = log.read_text(errors='replace')
            for marker, path in markers.items():
                if marker in text and marker not in captured:
                    # Loom can start a nested Xvfb. Capture the display used by Minecraft,
                    # not an empty outer display. These values come from our test mod.
                    env = dict(os.environ)
                    for key, label in [('DISPLAY', 'CTM_X_DISPLAY'), ('XAUTHORITY', 'CTM_X_AUTHORITY')]:
                        match = re.search(re.escape(label) + r'=([^\s]+)', text)
                        if match and match.group(1) != 'null': env[key] = match.group(1)
                    subprocess.run(['import', '-window', 'root', path], env=env, check=True, timeout=15)
                    captured.add(marker)
            time.sleep(.25)
        if process.poll() is None:
            raise RuntimeError('Render tests timed out')
    finally:
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
text = log.read_text(errors='replace')
print(text[-6000:])
if process.returncode != 0 or 'CTM_RENDER_TESTS_PASSED' not in text or len(captured) != len(markers):
    raise SystemExit('Enabled mixin and real-screen render tests failed; inspect the attached log')
print('PASS: enabled sidebar/tooltip assertions, actual sidebar rendering, settings frames and parent-screen return')
