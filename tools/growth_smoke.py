"""Opt-in integration check using one new disposable QA city.

Uses no existing credentials, files, admin permissions, or payments. The random
credentials are created solely for the selected server and remain in memory.
Run: python3 tools/growth_smoke.py https://YOUR-GATEWAY
"""
import concurrent.futures
import json
import secrets
import sys
import urllib.error
import urllib.request

origin = sys.argv[1].rstrip('/')
assert origin.startswith('https://')
token = ''

def request(path, body=None, key=None):
    headers = {'Content-Type': 'application/json', 'X-Device-ID': 'frontier-growth-qa', 'X-Device-Kind': 'android', 'X-Client-Version': '0.3.0'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if key:
        headers['X-Idempotency-Key'] = key
    req = urllib.request.Request(origin + '/api/v1' + path, headers=headers,
        data=None if body is None else json.dumps(body).encode())
    with urllib.request.urlopen(req, timeout=40) as response:
        return json.load(response)

health = request('/health')
assert health['server_id'] == 1 and health['version'] == 'core-v30'
account = {'username': 'growthqa' + secrets.token_hex(5), 'password': secrets.token_urlsafe(24)}
request('/register', account)
token = request('/login', account)['token']
del account
paths = ['/me','/commanders','/commanders/skills','/equipment','/shop/v3/catalog','/commerce/v2/wallet','/world/v2/state','/game/buildings']
docs = {'/commanders': request('/commanders')}
remaining = [p for p in paths if p != '/commanders']
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    docs.update(zip(remaining, pool.map(request, remaining)))
print('PASS all eight authenticated contracts', flush=True)
for p in paths:
    if p != '/me':
        print(p, json.dumps(docs[p], ensure_ascii=False), flush=True)
assert docs['/commerce/v2/wallet']['total_gems'] == 0
assert all(p['product_type'] in ('iap','gem_offer') for p in docs['/shop/v3/catalog']['products'])
before = docs['/me']['resources']
try:
    request('/game/training/start', {'type':'infantry','tier':5,'amount':1},secrets.token_hex(16))
    raise AssertionError('Locked tier was accepted')
except urllib.error.HTTPError as error:
    assert error.code == 400
    rejected = json.load(error)
    assert rejected.get('required_level') == 25, rejected
assert request('/me')['resources'] == before
print('PASS T5 requires building level 25 without charging resources',flush=True)
owned = {c['key'] for c in docs['/commanders']['commanders'] if c['owned']}
skill = next((s for s in docs['/commanders/skills']['skills'] if s['commander_key'] in owned and s['level'] < s['max_level'] and s['upgrade_gold']*max(1,s['level']) <= before['gold']),None)
if skill:
    key = secrets.token_hex(16)
    body = {'commander_key':skill['commander_key'],'skill_key':skill['skill_key']}
    first = request('/commanders/skills/upgrade',body,key)
    replay = request('/commanders/skills/upgrade',body,key)
    assert first == replay
    updated = next(s for s in request('/commanders/skills')['skills'] if s['commander_key']==body['commander_key'] and s['skill_key']==body['skill_key'])
    assert updated['level'] == max(1,skill['level'])+1
    print('PASS hero skill upgrade persists once with safe replay',flush=True)
else:
    print('SKIP hero mutation: no eligible owned skill on starter account',flush=True)
offer=next(p for p in docs['/shop/v3/catalog']['products'] if p['product_type']=='gem_offer')
try:
    request('/shop/v3/buy',{'product_key':offer['product_key'],'quantity':1},secrets.token_hex(16))
    raise AssertionError('Insufficient gem balance was accepted')
except urllib.error.HTTPError as error:
    assert error.code in (400,409)
    print('PASS shop rejects insufficient gem balance',flush=True)
assert request('/commerce/v2/wallet')['total_gems'] == 0
print('PASS no payments or currency grants performed',flush=True)
