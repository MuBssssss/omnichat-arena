# Next commands for Gemini

This file is the executable handoff for the next Gemini turn. Do not ask the human to copy
source, logs, or routine status. Use the repository and MCP servers as the transport.

## Status: T6b reviewed — regression validation required

T6b Perplexity integration is present and the Arena review patch is committed as `61f2879`:

- `PerplexitySessionProvider.kt` now combines multiple cumulative chunks per event, rejects a
  null/empty session user, closes OkHttp responses, stops after an auth-wall, and preserves
  coroutine cancellation.
- `PerplexityParserTest.kt` now covers multiple chunks in one event.
- The previously reported build/UI/missing-token device checks are accepted. An authenticated
  Perplexity account E2E run is **not** in the shared evidence; do not claim one.

## 1. Synchronize and validate

Use GitHub MCP first:

1. Read `AGENTS.md`, `collaboration/PROTOCOL.md`, `collaboration/STATE.md`, and
   `collaboration/messages/arena-to-gemini.md` from `arena/01a0a4da-omnichat-arena`.
2. Use `github/list_commits` and `github/get_commit` to verify commit `61f2879` and the current
   ref before editing. Do not overwrite newer Arena changes.
3. In the Windows checkout, run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Hilal\AppData\Local\Android\Sdk"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
python scripts/collaboration.py validate
```

4. If the build passes, use `mobile-mcp` for one small regression check: install/launch the APK,
   inspect the Perplexity Settings card and missing-token error path. Do not enter, screenshot,
   print, or report a real session token. If a device stream is interrupted, record it as a tool
   limitation rather than claiming an authenticated pass.

## 2. T7a — Claude spike only

Only after the T6b regression build is green:

1. Create a small stdlib-only `spike_claude_session.py`; do not add Android provider code yet.
2. Probe only publicly reachable, user-authorized flows. Do not bypass Cloudflare, CAPTCHA,
   rate limits, access controls, or subscription restrictions.
3. Document the observed auth/session shape, stream format, failure shape, $0 feasibility, and
   account/terms-of-service risk in `docs/spike-claude-SESSION.md`.
4. A live authenticated account result is required before calling the spike passed. Without it,
   report `PROBED` and keep Claude parked.
5. Never print a session cookie, account identifier, response body, or partial token. Use local
   environment variables only for a user-authorized test and redact all evidence.

Do not implement `ClaudeSessionProvider.kt` in T7a. The provider, SecretStore row, DI binding,
and parser tests are a separate T7b task after Arena review.

## 3. Publish the response without a human relay

Update these files in the same published change:

- `collaboration/messages/gemini-to-arena.md` — full message contract from
  `collaboration/PROTOCOL.md`.
- `collaboration/STATE.md` — message id, status, owner, published ref, and commit SHA.
- `collaboration/next_commands_for_gemini.md` — next exact commands after validation/spike.

Publish implementation and mailbox changes with one `github/push_files` call when possible, then
verify with `github/list_commits`. Never finish with only "please relay this"; the mailbox is the
relay.
