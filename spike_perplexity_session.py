#!/usr/bin/env python3
"""T6a probe for the Perplexity web-session SSE shape.

Stdlib only. This probe deliberately does not print prompts, response text, token fragments,
account identifiers, or response bodies. A real session token may be supplied only through the
local PERPLEXITY_SESSION_TOKEN environment variable; it is never an argument or a log value.

This script distinguishes an endpoint probe from an authenticated pass. A HTTP 200 response
without a verified session is not proof that a user's account can stream.

Usage:
  python spike_perplexity_session.py "Explain photosynthesis briefly"
  $env:PERPLEXITY_SESSION_TOKEN = "<local secret>"  # PowerShell, never commit this
  python spike_perplexity_session.py "Explain photosynthesis briefly"
"""

import json
import os
import ssl
import sys
import time
import urllib.error
import urllib.request
from typing import Optional, Tuple

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
)
SESSION_URL = "https://www.perplexity.ai/api/auth/session"
ASK_URL = "https://www.perplexity.ai/rest/sse/perplexity_ask"


def check_session(token: str) -> bool:
    """Return whether the session endpoint reports an authenticated user.

    Only the boolean result is returned. The response body is never printed because it can
    contain account data. The token itself is read from the environment and never logged.
    """
    headers = {
        "User-Agent": UA,
        "Accept": "application/json",
        "Origin": "https://www.perplexity.ai",
        "Referer": "https://www.perplexity.ai/",
        "Cookie": (
            "__Secure-next-auth.session-token="
            f"{token}; next-auth.session-token={token};"
        ),
    }
    request = urllib.request.Request(SESSION_URL, headers=headers)
    try:
        with urllib.request.urlopen(
            request, context=ssl.create_default_context(), timeout=15
        ) as response:
            if not (200 <= response.status < 300):
                return False
            payload = json.loads(response.read().decode("utf-8"))
            return bool(payload.get("user"))
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, ValueError):
        return False


def stream_ask(prompt: str, token: Optional[str]) -> Tuple[bool, bool]:
    """Probe SSE and return (text_observed, authwall_observed).

    Response text and event payloads are intentionally not printed. This keeps a local probe
    from leaking a user's prompt/answer into terminal history or a copied report.
    """
    headers = {
        "User-Agent": UA,
        "Accept": "text/event-stream",
        "Content-Type": "application/json",
        "Origin": "https://www.perplexity.ai",
        "Referer": "https://www.perplexity.ai/",
    }
    if token:
        headers["Cookie"] = (
            "__Secure-next-auth.session-token="
            f"{token}; next-auth.session-token={token};"
        )

    payload = {
        "query_str": prompt,
        "mode": "CONCISE",
        "model": "turbo",
        "source": "default",
    }
    request = urllib.request.Request(
        ASK_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
        method="POST",
    )

    started = time.time()
    first_text_ms = None
    accumulated_text = ""
    saw_text = False
    authwall = False
    event_count = 0

    try:
        with urllib.request.urlopen(
            request, context=ssl.create_default_context(), timeout=30
        ) as response:
            content_type = response.headers.get_content_type()
            if content_type != "text/event-stream":
                print(f"[!] Unexpected content type: {content_type}")

            for raw_line in response:
                line = raw_line.decode("utf-8", errors="replace").strip()
                if not line.startswith("data:"):
                    continue
                raw_json = line[5:].strip()
                if not raw_json:
                    continue
                try:
                    event = json.loads(raw_json)
                except json.JSONDecodeError:
                    continue
                event_count += 1

                upsell = event.get("upsell_information")
                if isinstance(upsell, dict) and upsell.get("name") == "fraud_authwall_upsell":
                    authwall = True

                for block in event.get("blocks", []):
                    if not isinstance(block, dict):
                        continue
                    markdown = block.get("markdown_block", {})
                    if not isinstance(markdown, dict):
                        continue
                    for chunk in markdown.get("chunks", []):
                        if not isinstance(chunk, str) or not chunk:
                            continue
                        if chunk.startswith(accumulated_text):
                            delta = chunk[len(accumulated_text):]
                            accumulated_text = chunk
                        elif chunk != accumulated_text:
                            # A provider update may restart or send a non-cumulative chunk.
                            delta = chunk
                            accumulated_text += chunk
                        else:
                            delta = ""
                        if delta:
                            saw_text = True
                            if first_text_ms is None:
                                first_text_ms = int((time.time() - started) * 1000)

            total_ms = int((time.time() - started) * 1000)
            print(
                f"[*] SSE probe completed: HTTP {response.status}, "
                f"events={event_count}, text_chars={len(accumulated_text)}, "
                f"ttft_ms={first_text_ms}, total_ms={total_ms}"
            )
            return saw_text, authwall
    except urllib.error.HTTPError as error:
        print(f"[!] SSE probe HTTP status: {error.code}")
        return False, False
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        # Do not include exception text: some transports echo request details.
        print(f"[!] SSE probe transport failure: {error.__class__.__name__}")
        return False, False


def main() -> int:
    prompt = sys.argv[1] if len(sys.argv) > 1 else "Explain how airplanes fly in one sentence."
    token = os.environ.get("PERPLEXITY_SESSION_TOKEN")
    session_active = False

    print("=" * 60)
    print("PERPLEXITY WEB-SESSION SPIKE (T6a)")
    print("=" * 60)
    if token:
        print("[1/2] Session token supplied through the local environment; value is not displayed.")
        session_active = check_session(token)
        print(f"      Session endpoint reports authenticated user: {session_active}")
    else:
        print("[1/2] Probe mode: no session token supplied.")

    # Only print endpoint and prompt length, not user content.
    print(f"[2/2] Probing SSE endpoint; prompt_chars={len(prompt)}")
    text_observed, authwall = stream_ask(prompt, token)

    print("=" * 60)
    if token and session_active and text_observed and not authwall:
        print("[RESULT] T6a SPIKE PASSED: authenticated session streaming observed.")
    elif authwall:
        print("[RESULT] T6a SPIKE PROBED: endpoint returned an authentication wall.")
    elif text_observed:
        print("[RESULT] T6a SPIKE PROBED: text observed, but authenticated session was not proven.")
    else:
        print("[RESULT] T6a SPIKE PROBED: endpoint/authentication shape only; no authenticated pass.")
    print("=" * 60)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
