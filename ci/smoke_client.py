"""Run actual client integrations, not substitute classes or mocked renderers."""
import os, signal, subprocess, sys
from pathlib import Path
mode = sys.argv[1] if len(sys.argv) > 1 else 'base'
if mode not in ('base', 'custom', 'hanni', 'both'): raise SystemExit('Unknown test mode')
log = Path('build/ci-client.log' if mode == 'base' else 'build/ci-' + mode + '.log')
log.parent.mkdir(exist_ok=True)
command = ['xvfb-run', '-a', 'gradle', 'runSmokeClient', '--no-daemon']
if mode != 'base': command += ['-PintegrationTest=' + mode]
with log.open('w') as output:
    process = subprocess.Popen(command, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
    try: process.wait(timeout=420)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGTERM)
        try: process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL); process.wait()
        raise SystemExit('Client integration timed out: ' + str(log))
text = log.read_text(errors='replace')
print(text[-18000:])
markers = ['CTM_ROW_TESTS_PASS', 'CTM_PURSE_DIAGNOSTIC_PASS', 'CTM_RENDER_TESTS_PASS', 'CTM_RESOURCE_COSTS_PASS']
if mode in ('custom', 'both'): markers.append('CTM_CUSTOMSCOREBOARD_PASS')
if mode in ('hanni', 'both'): markers.extend(['CTM_SKYHANNI_PASS', 'CTM_FINAL_INTEGRATION_CHECKS_PASS'])
if mode == 'base': markers.append('CTM_OPTIONAL_COMPAT_ABSENT_PASS')
if process.returncode or any(m not in text for m in markers): raise SystemExit('Failed integration: ' + mode)
print('PASS: actual client integration', mode)
