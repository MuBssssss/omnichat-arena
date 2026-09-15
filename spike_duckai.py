#!/usr/bin/env python3
"""Phase-0 spike: verify Duck.ai unofficial chat flow. Stdlib only.

Flow: GET /duckchat/v1/status (X-Vqd-Accept: 1) -> x-vqd-4 token
      POST /duckchat/v1/chat (x-vqd-4: <token>) -> SSE answer stream
Run: python3 spike_duckai.py "say hi in 5 words"
"""
import json
import sys
import urllib.request

UA = ("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36")
BASES = [
    "https://duckduckgo.com/duckchat/v1",
    "https://duck.ai/duckchat/v1",  # fallback if it moved
]

prompt = sys.argv[1] if len(sys.argv) > 1 else "Reply with exactly: spike OK"


def get_vqd(base: str) -> str:
    req = urllib.request.Request(base + "/status",
                                 headers={"X-Vqd-Accept": "1", "User-Agent": UA})
    with urllib.request.urlopen(req, timeout=20) as r:
        tok = r.headers.get("x-vqd-4") or r.headers.get("x-vqd-hash-1")
        if not tok:
            raise RuntimeError(f"no vqd token; headers={dict(r.headers)}")
        return tok


def chat(base: str, vqd: str, model: str) -> str:
    body = json.dumps({"model": model, "messages": [{"role": "user", "content": prompt}]}).encode()
    req = urllib.request.Request(
        base + "/chat", data=body,
        headers={"Content-Type": "application/json", "x-vqd-4": vqd, "User-Agent": UA})
    out = []
    with urllib.request.urlopen(req, timeout=60) as r:
        for raw in r:
            line = raw.decode("utf-8", "replace").strip()
            if not line.startswith("data:"):
                continue
            payload = line[5:].strip()
            if not payload or payload == "[DONE]":
                continue
            try:
                msg = json.loads(payload).get("message", "")
                out.append(msg)
                print(msg, end="", flush=True)
            except json.JSONDecodeError:
                print(f"\n[unparseable SSE: {payload[:120]}]")
    print()
    return "".join(out)


MODELS = ["gpt-4o-mini", "claude-3-haiku-20240307",
          "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo", "mistralai/Mixtral-8x7B-Instruct-v0.1"]

ok = False
for base in BASES:
    try:
        print(f"== trying {base} ==")
        vqd = get_vqd(base)
        print(f"vqd token: {vqd[:12]}... (len {len(vqd)})")
        for m in MODELS:
            try:
                print(f"-- model {m} --")
                ans = chat(base, vqd, m)
                if ans.strip():
                    print(f"\nSPIKE RESULT: OK base={base} model={m} chars={len(ans)}")
                    ok = True
                    break
            except Exception as e:  # noqa: BLE001
                print(f"model {m} failed: {e}")
        if ok:
            break
    except Exception as e:  # noqa: BLE001
        print(f"base {base} failed: {e}")

sys.exit(0 if ok else 1)
