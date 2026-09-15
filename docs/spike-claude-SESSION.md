# T7a Spike Specification — Claude Web Session Protocol

## Status: PROBED, parked (Cloudflare challenge / bot-wall)

The T7a probe investigated the viability of an unofficial web-session integration for Anthropic's
Claude.ai platform. In Windows direct Python/OkHttp testing, all direct HTTP requests to
`https://claude.ai` and `https://claude.ai/api/organizations` are intercepted by Cloudflare Bot
Management with HTTP 403 and `CF-RAY` challenge tokens.

Per project rules ($0-only, native UI only, no WebViews in chat, no paid solvers or bypass relays),
Claude is **parked** as bot-walled pending official free API access or documented clearance.

## 1. Observed protocol shape

- **Web URL:** `https://claude.ai`
- **Session endpoint:** `GET https://claude.ai/api/organizations`
- **Current Account endpoint:** `GET https://claude.ai/api/auth/current_account`
- **Chat endpoint:** `POST https://claude.ai/api/organizations/{org_uuid}/chat_conversations/{chat_uuid}/completion`
- **Protocol:** HTTP POST over TLS with Server-Sent Events (`text/event-stream`).
- **Bot-protection observation:** Direct non-browser HTTP clients receive HTTP 403 from Cloudflare
  edge servers (`server: cloudflare`, `cf-ray: <ray-id>`). A headless or automated client cannot
  establish a session without passing Cloudflare Turnstile/Managed Challenges and acquiring a valid
  `cf_clearance` cookie.

Run the probe for shape verification:

```text
python spike_claude_session.py "Explain photosynthesis briefly"
```

## 2. Authentication and security risks

1. **Cookie structure:** Claude.ai web sessions utilize `sessionKey=sk-ant-sid01-...` session
   cookies issued upon email/Google OAuth login.
2. **Bot defense binding:** The session cookie is bound to browser fingerprinting and Cloudflare
   clearance (`cf_clearance`). Extracting only the `sessionKey` into an Android OkHttp client fails
   at the Cloudflare edge with HTTP 403 before reaching Anthropic application endpoints.
3. **Terms of Service & account safety:** Attempting automated TLS fingerprint spoofing or
   third-party solving services introduces significant account ban risks and violates the $0
   architecture principle.
4. **Conclusion:** Claude session provider cannot run reliably in a pure native OkHttp client
   without triggering Cloudflare bot walls.

## 3. Architecture recommendation

- **Decision:** Keep Claude **parked** alongside DeepSeek and Duck.ai in `ProviderId.selectable`
  (`ProviderId.CLAUDE -> false`).
- **Next steps:** In Settings, show Claude as "⛔ bot-walled (Cloudflare challenge, Sep 2026)".
- **Next viable candidate:** Grok ($0 xAI / web-session probe) or Arena ranking engine enhancements.
