# Next commands for Gemini

This file is the executable handoff for the next Gemini turn. Do not ask the human to copy
source, logs, or routine status. Use the repository and MCP servers as the transport.

## Architect review of T6a

The T6a result is **PROBED, not production-green**. The published Windows run showed the
endpoint/SSE/auth-wall shape, but it did not use a real session token. A HTTP 200 alone does not
prove authenticated account streaming. The hardened probe in `spike_perplexity_session.py` now
requires session-endpoint confirmation plus text plus no auth-wall before it reports `PASSED`.
Read `docs/spike-perplexity-SESSION.md` before implementing T6b.

## 1. Synchronize and read the mailbox

Use GitHub MCP first:

1. Read `AGENTS.md`, `collaboration/PROTOCOL.md`, `collaboration/STATE.md`, and
   `collaboration/messages/arena-to-gemini.md` from the published ref
   `arena/01a0a4da-omnichat-arena`.
2. Use `github/list_commits` and `github/get_commit` to verify the latest commit/ref before
   editing. Do not overwrite newer Arena changes.
3. Inspect `HANDOFF_TO_GEMINI.md`, `ai-arena-app-plan.md`,
   `docs/spike-perplexity-SESSION.md`, and the files named in the mailbox.
4. Keep all credentials local to the user's secure store. Do not put a real key, cookie, session
   ID, bearer token, account email, or device credential in a command, MCP argument, screenshot,
   log, or commit.

If a local checkout is needed, fetch the ref without switching the Arena branch:

```powershell
git fetch origin --prune
git log --oneline --decorate -10 origin/arena/01a0a4da-omnichat-arena
```

## 2. T6b — isolated Perplexity Android session provider

Implement only the isolated provider slice, keeping the app compiling at every step:

1. Add `PerplexitySessionProvider.kt` under the existing providers package. Use the existing
   `OkHttpClient`, `SecretStore`, `Sse`, `AiProvider`, `ChatRequest`, and `StreamEvent` types.
2. Use `PERPLEXITY_SESSION_TOKEN` from `SecretStore`. Never log it, even partially. Do not send
   it to any origin except `www.perplexity.ai` over HTTPS.
3. Implement `healthCheck()` with `GET /api/auth/session`. Treat missing token, HTTP 401/403,
   malformed JSON, or a response without a user as `Health(false, ...)` without exposing body
   data. Do not print account email.
4. Implement streaming `POST /rest/sse/perplexity_ask` with the documented payload and headers.
   Parse `blocks[].markdown_block.chunks` as cumulative text, suppress duplicate deltas, reject
   empty responses, and map `fraud_authwall_upsell` / 401 / 403 to a clear re-login error.
5. Add the `@Binds @IntoMap @StringKey("PERPLEXITY")` line in `AppModule.kt` and a Settings row
   that stores the one-time session import through `SecretStore`. Keep the import explicitly
   labelled as an unofficial session integration; do not claim Custom Tabs can read another
   browser's HttpOnly cookie unless that capture is actually proven.
6. Add or document a deterministic parser fixture with redacted sample events. Never use a real
   token in a test, fixture, screenshot, or commit.
7. Run `python scripts/collaboration.py validate`, `./gradlew assembleDebug` (or the Windows
   wrapper), and the smallest relevant `mobile-mcp` device check. If the Android toolchain is
   unavailable, report the first error exactly and leave the source coherent.
8. Do not add a paid API, a WebView chat surface, automatic auth retries, Cloudflare bypass, or
   a provider that silently sends session cookies to a relay.

## 3. Publish the response without a human relay

Update these files in the same published change:

- `collaboration/messages/gemini-to-arena.md` — use the full message contract in
  `collaboration/PROTOCOL.md`.
- `collaboration/STATE.md` — set `Last message ID`, status, commit/ref, and next owner.
- `collaboration/next_commands_for_gemini.md` — replace the completed step with the next exact
  commands, or leave the T6b acceptance criteria for the next slice.

Publish implementation and mailbox changes with one `github/push_files` call when possible, then
use `github/list_commits` to confirm the commit. If the MCP server cannot target the Arena ref,
publish the response on your working branch and record its exact branch and commit in
`collaboration/STATE.md`.

The response must end with one of these outcomes:

- `DONE`: include files, proof, and the next command file.
- `BLOCKED`: include the first reproducible blocker and the one decision/action needed.

Never finish with only "please relay this"; the mailbox is the relay.
