import urllib.request
import json
import ssl

ctx = ssl.create_default_context()

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36',
    'Accept': '*/*',
    'Content-Type': 'application/json',
    'Origin': 'https://chat.deepseek.com',
    'Referer': 'https://chat.deepseek.com/',
}

data = json.dumps({'target_path': '/api/v0/chat/completion'}).encode('utf-8')
req = urllib.request.Request('https://chat.deepseek.com/api/v0/chat/create_pow_challenge', data=data, headers=headers)
try:
    with urllib.request.urlopen(req, context=ctx, timeout=10) as resp:
        print(f'POW Status: {resp.status}')
        print(f'Headers: {dict(resp.headers)}')
        print(f'Body: {resp.read().decode("utf-8")[:500]}')
except urllib.error.HTTPError as e:
    print(f'POW HTTP Error: {e.code} {e.reason}')
    print(f'Headers: {dict(e.headers)}')
    print(f'Body: {e.read().decode("utf-8", errors="replace")[:500]}')
except Exception as e:
    print(f'POW Error: {e}')
