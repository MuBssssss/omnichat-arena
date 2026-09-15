# T6a Spike Specification — Perplexity Web Session Protocol

## Status: PROBED, not production-green

The spike documented a reachable SSE endpoint and the shape of its unauthenticated response in
Gemini's Windows environment. It did **not** prove an authenticated Perplexity account stream:
no real session token was used in the published proof. A HTTP 200 from the stream endpoint is
therefore endpoint evidence, not a session-integration pass. Run the hardened probe with a
local session only when the account owner explicitly chooses to test it.

The committed probe is intentionally secret-safe: it reads a token only from the local
`PERPLEXITY_SESSION_TOKEN` environment variable, never prints token fragments or account data,
and reports only status/shape metrics.

## 1. Observed protocol shape

- **Web URL:** `https://www.perplexity.ai`
- **Stream endpoint:** `POST https://www.perplexity.ai/rest/sse/perplexity_ask`
- **Session endpoint:** `GET https://www.perplexity.ai/api/auth/session`
- **Protocol:** HTTP POST over TLS with Server-Sent Events (`text/event-stream`).
- **Bot-protection observation:** Gemini observed a Cloudflare challenge on a bare root GET,
  while the REST stream probe returned HTTP 200 in that environment. This is an observation, not
  a guarantee that the endpoint will remain available or that it may be used to bypass controls.

Run the probe without a token for shape-only evidence:

```text
python spike_perplexity_session.py "Explain photosynthesis briefly"
```

An authenticated run may use a token in the local environment only. Never put a real token in a
command transcript, source file, screenshot, issue, commit, or mailbox:

```powershell
$env:PERPLEXITY_SESSION_TOKEN = "<local secret>"
python spike_perplexity_session.py "Explain photosynthesis briefly"
Remove-Item Env:PERPLEXITY_SESSION_TOKEN
```

A `PASSED` result requires all of the following: the session endpoint reports an authenticated
user, the stream produces text, and no auth-wall event is observed. Any other result is
`PROBED`, not a green light for an Android provider.

## 2. Authentication and storage

The observed session-cookie shape was:

1. `__Secure-next-auth.session-token` — account session cookie.
2. `next-auth.csrf-token` — may be relevant to other web mutations; do not assume it is
   optional outside the tested read/stream request.

Security rules:

- Cookies MUST NEVER be checked into Git, written to unencrypted storage, printed, partially
  masked in logs, or included in MCP arguments.
- Android must store any accepted session value only in the encrypted `SecretStore`.
- A session token is not an API key and must never be sent to any host other than the provider
  origin. Avoid logging OkHttp headers and exception bodies.
- The app must show a clear re-login/session-expired error and must not retry an auth failure in a
  tight loop.

## 3. Request and response shape observed in the probe

```http
POST /rest/sse/perplexity_ask HTTP/1.1
Host: www.perplexity.ai
User-Agent: <browser-like user agent>
Content-Type: application/json
Accept: text/event-stream
Origin: https://www.perplexity.ai
Referer: https://www.perplexity.ai/
Cookie: __Secure-next-auth.session-token=<SESSION_TOKEN>;
```

```json
{
  "query_str": "Explain quantum computing briefly",
  "mode": "CONCISE",
  "model": "turbo",
  "source": "default"
}
```

The observed event shape included cumulative markdown chunks:

```text
event: message
data: {"blocks":[{"markdown_block":{"chunks":["..."]}}]}
```

The unauthenticated response was observed to include an
`upsell_information.name` value of `fraud_authwall_upsell`. Treat this as a failure state,
not as a response to display as an answer.

## 4. T6b Android implementation gate

T6b may implement an isolated provider only after the following are true:

1. Keep a small, deterministic parser test/fixture for the event shape; do not test with a real
   cookie in Git or CI.
2. Add `PerplexitySessionProvider.kt` with `healthCheck()` against the session endpoint and
   streaming through the existing `Sse` helper. Parse cumulative chunks defensively and emit one
   `StreamEvent.Done` only after non-empty text.
3. Use `SecretStore` keys such as `PERPLEXITY_SESSION_TOKEN` (and an optional CSRF value only if
   a tested request requires it). Never expose the stored value after save.
4. Add the provider's `@Binds` line and a Settings row with a clear session-risk warning. Do not
   claim that a normal Custom Tab can read another browser's HttpOnly cookie; if automatic login
   capture cannot be proven, label the one-time session import as manual and keep chat native.
5. Map HTTP 401/403 and `fraud_authwall_upsell` to a non-crashing "re-login required" error.
   Do not add retries that could trigger account protection.
6. Build, run the collaboration validator, and use `mobile-mcp` for the smallest relevant device
   check. Record only non-sensitive proof in the mailbox.

This remains an unofficial web-session integration with possible terms-of-service and account
risk. It must stay opt-in, $0-only, rate-limited, and easy to disable.
