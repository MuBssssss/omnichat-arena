# T8a Spike — Grok web-session feasibility

## Status: PROBED — not authenticated, not provider-green

Jules' Windows probe found that the public Grok home page returned HTTP 200 without an immediate
Cloudflare challenge, while the guessed `/api/rpc` route returned an HTTP 404/401/403 shape. This
is only reachability evidence: it does not identify the current chat endpoint and does not prove
that a user session can be replayed from native OkHttp.

## Observed evidence

- Home: `GET https://grok.com/`
- Initial API-shape guess: `POST https://grok.com/api/rpc`
- A normal page load may set browser/device and Cloudflare management cookies. The probe discards
  them and never prints or stores them.
- No authenticated token or account was used in the published proof.
- Arena's sandbox probe returned a local TLS transport EOF, so the Windows observation remains the
  only live network evidence in this record.

## Security and scope rules

- Do not add `GrokSessionProvider` from this probe.
- Do not extract or paste cookies, use browser-fingerprint spoofing, solve CAPTCHA/Cloudflare,
  bypass rate limits, or use a paid relay/solver.
- If a future user-authorized test is attempted, read secrets only from local secure storage or an
  environment variable and never print even partial values. Keep native chat separate from any
  login surface.
- Grok web-session automation has account and terms-of-service risk; it remains opt-in and easy to
  park.

## Next decision

A T8b provider is **not approved** yet. First identify a current, user-authorized, reproducible
chat protocol without bypassing access controls. Otherwise keep Grok parked and prioritize Arena
judge/UI/persistence improvements.
