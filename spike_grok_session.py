#!/usr/bin/env python3
"""T8a probe for public Grok web reachability.

Stdlib only. This is a shape/reachability probe, not a login automation tool. It never accepts a
session token, prints response bodies, or attempts to bypass Cloudflare, CAPTCHA, rate limits,
access controls, or subscription restrictions.
"""

import ssl
import urllib.error
import urllib.request

UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
)
HOME_URL = "https://grok.com/"
API_GUESS_URL = "https://grok.com/api/rpc"


def probe(url: str, method: str = "GET") -> tuple[int, bool, str]:
    """Return (status, cloudflare_hint, transport_result) without exposing response data."""
    request = urllib.request.Request(
        url,
        method=method,
        headers={
            "User-Agent": UA,
            "Accept": "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
        },
    )
    try:
        with urllib.request.urlopen(
            request, context=ssl.create_default_context(), timeout=15
        ) as response:
            server = response.headers.get("Server", "")
            cf_hint = bool(response.headers.get("CF-RAY") or "cloudflare" in server.lower())
            # Consume and discard the body so the connection is cleanly released. Never print it.
            response.read()
            return response.status, cf_hint, "ok"
    except urllib.error.HTTPError as error:
        headers = error.headers or {}
        server = headers.get("Server", "")
        cf_hint = bool(headers.get("CF-RAY") or "cloudflare" in server.lower())
        return error.code, cf_hint, "http-error"
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        # The exception text can contain transport/request details; keep reports generic.
        return 0, False, error.__class__.__name__


def main() -> int:
    print("=" * 60)
    print("GROK WEB-SESSION SPIKE (T8a)")
    print("=" * 60)

    home_status, home_cf, home_result = probe(HOME_URL)
    print(f"Home: HTTP {home_status}, cloudflare_hint={home_cf}, result={home_result}")

    api_status, api_cf, api_result = probe(API_GUESS_URL, method="POST")
    print(f"API shape probe: HTTP {api_status}, cloudflare_hint={api_cf}, result={api_result}")

    print("-" * 60)
    if home_status == 200 and not home_cf and api_status in (401, 403, 404):
        print("[RESULT] T8a PROBED: home is reachable; API/auth route is not established.")
    elif home_cf or api_cf:
        print("[RESULT] T8a PROBED: Cloudflare protection observed; keep Grok parked.")
    else:
        print("[RESULT] T8a PROBED: reachability shape only; no authenticated pass.")
    print("=" * 60)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
