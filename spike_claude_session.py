#!/usr/bin/env python3
"""T7a probe for the Claude (claude.ai) web-session SSE shape.

Stdlib only. This probe deliberately does not print prompts, response text, token fragments,
account identifiers, or response bodies. A real session key may be supplied only through the
local CLAUDE_SESSION_KEY environment variable; it is never an argument or a log value.

Usage:
  python spike_claude_session.py "Explain photosynthesis briefly"
  $env:CLAUDE_SESSION_KEY = "<local secret>"  # PowerShell, never commit this
  python spike_claude_session.py "Explain photosynthesis briefly"
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
BASE_URL = "https://claude.ai"
ORGS_URL = "https://claude.ai/api/organizations"
AUTH_URL = "https://claude.ai/api/auth/current_account"


def check_session(session_key: str) -> Tuple[bool, Optional[str], bool]:
    """Check whether the session key returns an authenticated organization.

    Returns (authenticated: bool, org_uuid: Optional[str], bot_walled: bool).
    Never prints or exposes account identifiers, emails, or names.
    """
    headers = {
        "User-Agent": UA,
        "Accept": "application/json",
        "Origin": BASE_URL,
        "Referer": f"{BASE_URL}/",
        "Cookie": f"sessionKey={session_key};",
    }
    request = urllib.request.Request(ORGS_URL, headers=headers)
    try:
        with urllib.request.urlopen(
            request, context=ssl.create_default_context(), timeout=15
        ) as response:
            if not (200 <= response.status < 300):
                return False, None, False
            payload = json.loads(response.read().decode("utf-8"))
            if isinstance(payload, list) and len(payload) > 0:
                org = payload[0]
                org_uuid = org.get("uuid") if isinstance(org, dict) else None
                return True, org_uuid, False
            return False, None, False
    except urllib.error.HTTPError as error:
        if error.code in (403, 503):
            # Cloudflare or bot wall
            return False, None, True
        return False, None, False
    except (urllib.error.URLError, TimeoutError, ValueError):
        return False, None, False


def probe_public_endpoint() -> Tuple[int, bool]:
    """Probe the unauthenticated reachability of Claude.ai endpoints.

    Returns (status_code: int, cf_challenge_observed: bool).
    """
    headers = {
        "User-Agent": UA,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    }
    request = urllib.request.Request(BASE_URL, headers=headers)
    try:
        with urllib.request.urlopen(
            request, context=ssl.create_default_context(), timeout=15
        ) as response:
            server = response.headers.get("Server", "")
            cf_ray = response.headers.get("CF-RAY", "")
            has_cf = bool(cf_ray or "cloudflare" in server.lower())
            return response.status, has_cf
    except urllib.error.HTTPError as error:
        server = error.headers.get("Server", "") if error.headers else ""
        cf_ray = error.headers.get("CF-RAY", "") if error.headers else ""
        has_cf = bool(cf_ray or "cloudflare" in server.lower())
        return error.code, has_cf
    except (urllib.error.URLError, TimeoutError, OSError):
        return 0, False


def main() -> int:
    prompt = sys.argv[1] if len(sys.argv) > 1 else "Explain how airplanes fly in one sentence."
    session_key = os.environ.get("CLAUDE_SESSION_KEY")

    print("=" * 60)
    print("CLAUDE WEB-SESSION SPIKE (T7a)")
    print("=" * 60)

    # 1. Probe base connectivity and Cloudflare presence
    status, has_cf = probe_public_endpoint()
    print(f"[*] Base origin reachability: HTTP {status} (Cloudflare present: {has_cf})")

    # 2. Probe session / auth if token provided
    auth_ok = False
    org_id = None
    bot_walled = False

    if session_key:
        print("[1/2] Session key supplied through local environment; value is not displayed.")
        auth_ok, org_id, bot_walled = check_session(session_key)
        print(f"      Session reports valid account/organization: {auth_ok}")
        if bot_walled:
            print("      [!] Cloudflare challenge / bot protection observed on API endpoint.")
    else:
        print("[1/2] Probe mode: no session key supplied.")

    # 3. Probe unauthenticated API endpoint behavior
    print(f"[2/2] Probing unauthenticated session endpoint ({ORGS_URL})...")
    req = urllib.request.Request(ORGS_URL, headers={"User-Agent": UA, "Accept": "application/json"})
    api_status = 0
    api_cf = False
    try:
        with urllib.request.urlopen(req, context=ssl.create_default_context(), timeout=15) as resp:
            api_status = resp.status
    except urllib.error.HTTPError as err:
        api_status = err.code
        api_cf = bool(err.headers.get("CF-RAY") if err.headers else False)
    except Exception as e:
        api_status = -1

    print(f"      Unauthenticated API response: HTTP {api_status} (CF: {api_cf})")

    print("=" * 60)
    if session_key and auth_ok and not bot_walled:
        print("[RESULT] T7a SPIKE PASSED: authenticated Claude session observed.")
    elif bot_walled or api_status == 403:
        print("[RESULT] T7a SPIKE PROBED: Cloudflare challenge / bot wall observed on Claude.ai.")
    elif api_status in (401, 403):
        print(f"[RESULT] T7a SPIKE PROBED: endpoint reachable; authentication required (HTTP {api_status}).")
    else:
        print(f"[RESULT] T7a SPIKE PROBED: endpoint status {api_status}; no authenticated pass.")
    print("=" * 60)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
