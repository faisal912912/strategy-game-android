"""Opt-in Core v30 gameplay contract check. Uses only its own randomly named test account.

Run: python3 tools/live_smoke.py https://YOUR-GATEWAY
Creates a small test city; never accesses admin routes, chat, or other players.
Credentials stay in a private local file excluded from git. Do not run as a load test.
"""
import concurrent.futures
import json
import pathlib
import secrets
import sys
import time
import urllib.error
import urllib.request

origin = sys.argv[1].rstrip('/')
assert origin.startswith('https://')
token = ''

def request(path, body=None, key=None):
    headers = {'Content-Type': 'application/json', 'X-Device-ID': 'frontier-qa', 'X-Device-Kind': 'android'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if key:
        headers['X-Idempotency-Key'] = key
    req = urllib.request.Request(origin + '/api/v1' + path, headers=headers,
        data=None if body is None else json.dumps(body).encode())
    with urllib.request.urlopen(req, timeout=40) as response:
        return json.load(response)

def command(path, body):
    key = secrets.token_hex(16)
    first = request(path, body, key)
    replay = request(path, body, key)
    assert first == replay, 'Replay changed response: ' + path
    print('PASS idempotent', path, flush=True)
    return first

directory = pathlib.Path('.local-qa')
directory.mkdir(exist_ok=True, mode=0o700)
account_file = directory / 'account.json'
if account_file.exists():
    account = json.loads(account_file.read_text())
else:
    account = {'username': 'frontierqa' + secrets.token_hex(4), 'password': secrets.token_urlsafe(20)}
    request('/register', account)
    account_file.write_text(json.dumps(account)); account_file.chmod(0o600)
token = request('/login', account)['token']
assert request('/health')['server_id'] == 1
initial = request('/me')
print('PASS authentication and server identity', flush=True)
training = command('/game/training/start', {'type': 'infantry', 'tier': 1, 'amount': 100})
building = command('/game/buildings/upgrade', {'building': 'farm'})
nodes = request('/world/resources')['nodes']
node = next(n for n in nodes if n['type'] == 'gold' and n['amount'] >= 1000)
gather = command('/world/gather/start', {'node_id': node['id'], 'amount': 1000})
monsters = request('/world/monsters')['monsters']
monster = next(m for m in monsters if m['available'] and m['level'] == 1)
hunt = command('/world/monsters/hunt', {'monster_id': monster['id'], 'infantry': 50, 'cavalry': 50, 'archers': 50})
# Timers are short. The runner is resumable by the orchestration tool while it waits.
from datetime import datetime, timezone
deadline = max(datetime.fromisoformat(j['finishes_at']).timestamp() for j in [training, building, gather, hunt])
while time.time() < deadline + 2:
    time.sleep(min(5, max(0.1, deadline + 2 - time.time())))
command('/game/training/claim', {})
command('/game/buildings/claim', {})
command('/world/gather/claim', {'id': gather['job_id']})
result = command('/world/monsters/claim', {'id': hunt['hunt_id']})
assert result['success'] is True
final = request('/me')
assert final['army']['infantry'] >= initial['army']['infantry'], 'Training/hunt return missing'
assert next(b for b in request('/game/buildings')['buildings'] if b['type'] == 'farm')['level'] >= 2
request('/logout', {})
token = request('/login', account)['token']
restored = request('/me')
assert restored['army'] == final['army'], 'Army did not survive relogin'
report = {'checks': ['authentication', 'server identity', 'training', 'building', 'gathering', 'hunt victory', 'idempotent replay', 'relogin persistence'],
    'passed': True, 'timestamp': datetime.now(timezone.utc).isoformat(), 'hunt': result}
(directory / 'result.json').write_text(json.dumps(report, indent=2))
print(json.dumps(report), flush=True)
