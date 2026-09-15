# T6a Spike Specification — Perplexity Web Session Protocol

## 1. Overview & Protocol Architecture
- **Web URL:** https://www.perplexity.ai
- **Stream Generation Endpoint:** POST https://www.perplexity.ai/rest/sse/perplexity_ask
- **Session Verification Endpoint:** GET https://www.perplexity.ai/api/auth/session
- **Protocol:** HTTP POST over TLS with Server-Sent Events (text/event-stream) streaming.
- **Bot Protection Analysis:** 
  - Root path (https://www.perplexity.ai/) enforces Cloudflare Turnstile challenges (Cf-Mitigated: challenge) on bare GET requests.
  - The REST streaming endpoint (/rest/sse/perplexity_ask) accepts native OkHttp/Python HTTPS requests with standard browser headers without Cloudflare bot-walling (Cf-Mitigated: None, HTTP 200).

## 2. Authentication & Session Cookies
Authentication is session-cookie based (NextAuth architecture):
1. `__Secure-next-auth.session-token`: Core account session identifier (JWT).
2. `next-auth.csrf-token`: Anti-CSRF token (optional for /rest/sse/perplexity_ask).

### Security Rules
- Cookies MUST NEVER be checked into Git, written to unencrypted storage, or printed in logs.
- In Android, cookies are stored in encrypted SecretStore (MasterKey AES-256-GCM).

## 3. Request & Response Specification
### Request Headers
```http
POST /rest/sse/perplexity_ask HTTP/1.1
Host: www.perplexity.ai
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ...
Content-Type: application/json
Accept: text/event-stream
Origin: https://www.perplexity.ai
Referer: https://www.perplexity.ai/
Cookie: __Secure-next-auth.session-token=<SESSION_TOKEN>;
```

### Request Payload
```json
{
  "query_str": "Explain quantum computing briefly",
  "mode": "CONCISE",
  "model": "turbo",
  "source": "default"
}
```

### Streaming Response Format
- **Content-Type:** text/event-stream; charset=utf-8
- **Event Shape:**
  ```
  event: message
  data: {"backend_uuid": "...", "blocks": [{"intended_usage": "ask_text", "markdown_block": {"chunks": ["..."]}}], "status": "COMPLETED"}
  ```
- Incremental text deltas are extracted by taking `chunk[len(accumulated_text):]`.
- If unauthenticated, server returns `upsell_information: {"name": "fraud_authwall_upsell"}` and chunks contain "Sign up and repeat your request".

## 4. Android Implementation Roadmap (T6b)
1. Add `ProviderId.PERPLEXITY` with display name "Perplexity (Web Session)".
2. Add Settings card in `SettingsScreen.kt` for `__Secure-next-auth.session-token`.
3. Implement `PerplexitySessionProvider.kt` using standard OkHttp SSE streaming with `query_str`.
4. Validate session liveness via `GET /api/auth/session`.
