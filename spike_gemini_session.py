import os
import sys
import asyncio
import time

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

try:
    from gemini_webapi import GeminiClient
    from gemini_webapi.exceptions import AuthError, APIError
except ImportError:
    print("[ERROR] gemini_webapi not found. Run: pip install -U gemini_webapi")
    sys.exit(1)

async def main():
    psid = os.environ.get("GEMINI_PSID")
    psidts = os.environ.get("GEMINI_PSIDTS")

    if not psid or not psidts:
        print("=" * 60)
        print("[!] MISSING REQUIRED ENVIRONMENT VARIABLES")
        print("=" * 60)
        print("To run this spike securely without storing cookies on disk,")
        print("set these two variables in PowerShell before running:")
        print()
        print('  $env:GEMINI_PSID   = "__Secure-1PSID cookie value (starts with g.)"')
        print('  $env:GEMINI_PSIDTS = "__Secure-1PSIDTS cookie value"')
        print()
        print("Optional:")
        print('  $env:GEMINI_SAPISID = "SAPISID cookie value"')
        print("=" * 60)
        sys.exit(1)

    print("=" * 60)
    print("GEMINI PRO SESSION SPIKE (T3a)")
    print("=" * 60)
    print("[1/3] Initializing client with session cookies from env...")
    
    masked_psid = psid[:6] + "..." + psid[-4:] if len(psid) > 10 else "***"
    print(f"      PSID: {masked_psid}")

    client = GeminiClient(secure_1psid=psid, secure_1psidts=psidts)

    try:
        t0 = time.time()
        await client.init(timeout=30)
        init_ms = int((time.time() - t0) * 1000)
        print(f"      Connected! SNlM0e token scraped in {init_ms}ms.")
    except AuthError as e:
        print(f"[ERROR] AUTH ERROR: Session rejected or expired: {e}")
        print("Please refresh gemini.google.com in Chrome and re-copy cookies.")
        sys.exit(2)
    except Exception as e:
        print(f"[ERROR] CONNECTION ERROR: {type(e).__name__}: {e}")
        sys.exit(2)

    print("\n[2/3] Discovering account models via otAQ7b RPC...")
    models = client.list_models()
    if models:
        print(f"      Discovered {len(models)} model(s):")
        for idx, m in enumerate(models):
            name = getattr(m, "name", str(m))
            desc = getattr(m, "description", "")
            print(f"       [{idx+1}] {name} - {desc}")
    else:
        print("      No explicit model list returned; account using Google server-default.")

    test_prompt = "Explain in one sentence what makes you unique as Gemini Pro."
    print(f"\n[3/3] Testing live streaming generation...")
    print(f"      Prompt: \"{test_prompt}\"")
    print("      Stream output: ", end="", flush=True)

    stream_t0 = time.time()
    full_text = []
    try:
        async for chunk in client.generate_content_stream(test_prompt):
            if chunk.text_delta:
                print(chunk.text_delta, end="", flush=True)
                full_text.append(chunk.text_delta)
        stream_ms = int((time.time() - stream_t0) * 1000)
        print(f"\n\n[SUCCESS] Stream finished successfully in {stream_ms}ms ({len(''.join(full_text))} chars).")
    except APIError as e:
        print(f"\n[ERROR] STREAM ERROR: {e}")
        sys.exit(3)
    except Exception as e:
        print(f"\n[ERROR] UNEXPECTED ERROR: {type(e).__name__}: {e}")
        sys.exit(3)

    print("\n" + "=" * 60)
    print("[PASSED] T3a SPIKE PASSED - Ready for Android OkHttp port (T3b)")
    print("=" * 60)

if __name__ == "__main__":
    asyncio.run(main())
