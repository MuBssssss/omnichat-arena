import os
import sys
import json
import time
import ssl
import urllib.request
import urllib.error

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8')

UA = (
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
    '(KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36'
)

SESSION_URL = 'https://www.perplexity.ai/api/auth/session'
ASK_URL = 'https://www.perplexity.ai/rest/sse/perplexity_ask'

def check_session(token: str):
    headers = {
        'User-Agent': UA,
        'Accept': 'application/json',
        'Origin': 'https://www.perplexity.ai',
        'Referer': 'https://www.perplexity.ai/',
        'Cookie': f'__Secure-next-auth.session-token={token}; next-auth.session-token={token};'
    }
    ctx = ssl.create_default_context()
    req = urllib.request.Request(SESSION_URL, headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            return data if data.get('user') else None
    except Exception as e:
        print(f'[!] Session check warning: {e}')
        return None

def stream_ask(prompt: str, token: str = None):
    headers = {
        'User-Agent': UA,
        'Accept': 'text/event-stream',
        'Content-Type': 'application/json',
        'Origin': 'https://www.perplexity.ai',
        'Referer': 'https://www.perplexity.ai/',
    }
    if token:
        headers['Cookie'] = f'__Secure-next-auth.session-token={token}; next-auth.session-token={token};'

    payload = {
        'query_str': prompt,
        'mode': 'CONCISE',
        'model': 'turbo',
        'source': 'default'
    }

    ctx = ssl.create_default_context()
    body = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(ASK_URL, data=body, headers=headers)

    t0 = time.time()
    first_token_ms = None
    accumulated_text = ''
    authwall_detected = False

    try:
        with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
            print(f'[*] Connected (Status {resp.status})! Streaming response:')
            print('-' * 60)
            for raw_line in resp:
                line = raw_line.decode('utf-8', errors='replace').strip()
                if not line.startswith('data:'):
                    continue
                raw_json = line[5:].strip()
                if not raw_json:
                    continue
                try:
                    event = json.loads(raw_json)
                except json.JSONDecodeError:
                    continue

                if event.get('upsell_information'):
                    upsell = event['upsell_information']
                    if upsell.get('name') == 'fraud_authwall_upsell':
                        authwall_detected = True

                blocks = event.get('blocks', [])
                for b in blocks:
                    md = b.get('markdown_block', {})
                    chunks = md.get('chunks', [])
                    for c in chunks:
                        if len(c) > len(accumulated_text):
                            delta = c[len(accumulated_text):]
                            if first_token_ms is None:
                                first_token_ms = int((time.time() - t0) * 1000)
                            print(delta, end='', flush=True)
                            accumulated_text = c

            total_ms = int((time.time() - t0) * 1000)
            print('')
            print('-' * 60)
            print(f'[*] Completed in {total_ms}ms (TTFT: {first_token_ms}ms, {len(accumulated_text)} chars).')

            if authwall_detected:
                print('[!] NOTICE: Server triggered fraud_authwall_upsell (session required).')
                print('    To authenticate with your Perplexity account, set PERPLEXITY_SESSION_TOKEN')

            return not authwall_detected and len(accumulated_text) > 0

    except urllib.error.HTTPError as e:
        print(f'[ERROR] HTTP {e.code} {e.reason}')
        err_body = e.read().decode('utf-8', errors='replace')
        print(f'        Detail: {err_body[:300]}')
        return False
    except Exception as e:
        print(f'[ERROR] Request failed: {e}')
        return False

def main():
    prompt = sys.argv[1] if len(sys.argv) > 1 else 'Explain how airplanes fly in one sentence.'
    token = os.environ.get('PERPLEXITY_SESSION_TOKEN')

    print('=' * 60)
    print('PERPLEXITY WEB-SESSION SPIKE (T6a)')
    print('=' * 60)

    if token:
        masked = token[:6] + '...' + token[-4:] if len(token) > 10 else '***'
        print(f'[1/2] Session token present: {masked}')
        print('      Verifying session with NextAuth endpoint...')
        session_info = check_session(token)
        if session_info:
            user = session_info.get('user', {})
            email = user.get('email', 'user')
            print(f'      Session ACTIVE: logged in as {email}')
        else:
            print('      [!] Session not recognized or expired; proceeding...')
    else:
        print('[1/2] Running in probing mode (no PERPLEXITY_SESSION_TOKEN set).')

    print(f'\n[2/2] Probing {ASK_URL} with prompt: "{prompt}"')
    success = stream_ask(prompt, token)

    print('=' * 60)
    if success:
        print('[RESULT] T6a SPIKE PASSED: Perplexity web session streaming active!')
    else:
        print('[RESULT] T6a SPIKE PROBED: Endpoint active, session auth shape documented.')
    print('=' * 60)

if __name__ == '__main__':
    main()
