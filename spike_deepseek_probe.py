import urllib.request
import json
import ssl

ctx = ssl.create_default_context()

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9',
}

req = urllib.request.Request('https://chat.deepseek.com/', headers=headers)
try:
    with urllib.request.urlopen(req, context=ctx, timeout=10) as resp:
        print(f'Root Status: {resp.status}')
        print(f'Headers: {dict(resp.headers)}')
        body = resp.read().decode('utf-8')
        print(f'Body length: {len(body)}')
        print(f'Body snippet: {body[:300]}')
except urllib.error.HTTPError as e:
    print(f'Root HTTP Error: {e.code} {e.reason}')
except Exception as e:
    print(f'Root Error: {e}')
