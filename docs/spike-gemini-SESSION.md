# T3a Spike Specification — Gemini Pro Web Session Protocol

## 1. Overview & Protocol Architecture
- **Web UI URL:** `https://gemini.google.com/app`
- **Stream Generation Endpoint:** `POST https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate`
- **Model Discovery RPC:** `GET/POST https://gemini.google.com/_/BardChatUi/data/batchexecute?rpcids=otAQ7b`
- **Protocol:** Google `batchexecute` / chunked length-prefixed streaming format over HTTP/2.

## 2. Authentication & Session Cookies
Authentication does not use an API key. It uses browser session cookies:
1. `__Secure-1PSID`: Core account session identifier (starts with `g.`).
2. `__Secure-1PSIDTS`: Rolling timestamp token required for active verification.
3. `SAPISID` / `__Secure-1PAPISID`: Used for `SAPISIDHASH` authorization header where applicable.

### Security Rules
- Cookies MUST NEVER be checked into Git, written to disk, or sent in chat transcripts.
- In Android, cookies are kept in encrypted `SecretStore` (MasterKey AES-256-GCM).

## 3. Bootstrap & Token Flow
1. **Scrape `SNlM0e`:**
   - Request `GET https://gemini.google.com/app` with browser headers and cookies.
   - Extract `SNlM0e` (session access token) and build label `bl` from HTML using regex: `"SNlM0e":"([^"]+)"`.
2. **Discover Models (`otAQ7b`):**
   - Query `batchexecute?rpcids=otAQ7b` using the `f.req` envelope with `at=<SNlM0e>`.
   - Parse returned model descriptors and capabilities.
3. **Execute Stream (`StreamGenerate`):**
   - Submit prompt in nested JSON `f.req` array.
   - Stream response chunks line-by-line; extract text deltas incrementally.

## 4. Session Lifetime & Expiry Handling
- `__Secure-1PSIDTS` rotates periodically (~5–15 minutes of activity).
- Heartbeat rotation can be sustained via `https://accounts.google.com/RotateCookies`.
- On 401 or auth redirect: trigger re-authentication via in-app Chrome Custom Tab.
