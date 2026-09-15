# Next commands for Gemini

This file is the executable handoff for the next Gemini turn. Do not ask the human to copy
source, logs, or routine status. Use the repository and MCP servers as the transport.

## Status: T6b Complete — Awaiting Arena Review

T6b Perplexity Android session provider slice has been implemented and verified:
- `PerplexitySessionProvider.kt` with deterministic `PerplexityParser`
- `AppModule.kt` Dagger multi-binding
- `SettingsScreen.kt` session card and ViewModel storage
- `PerplexityParserTest.kt` unit test suite (BUILD SUCCESSFUL)
- Device verification on `SM-J701F` via `mobile-mcp` (Keys card & Chat selector verified)

## 1. Synchronize and read the mailbox

Use GitHub MCP first:

1. Read `AGENTS.md`, `collaboration/PROTOCOL.md`, `collaboration/STATE.md`, and
   `collaboration/messages/arena-to-gemini.md` from the published ref
   `arena/01a0a4da-omnichat-arena`.
2. Inspect `HANDOFF_TO_GEMINI.md`, `ai-arena-app-plan.md`, and the files named in the mailbox.
3. Keep all credentials local to the user's secure store. Do not put a real key, cookie, session
   ID, bearer token, account email, or device credential in a command, MCP argument, screenshot,
   log, or commit.

## 2. Next Sprint Target: T7 — Claude Web Session Integration

Once Arena architect approves T6b, proceed with T7 per plan §9:

1. Run isolated spike probe for Claude web session (`spike_claude_session.py`).
2. Document session cookie protocol, CSRF requirements, and SSE stream endpoints in `docs/spike-claude-SESSION.md`.
3. Implement `ClaudeSessionProvider.kt`, DI binding, Settings session card, and parser unit tests.
4. Verify with `./gradlew assembleDebug` and `mobile-mcp` on `SM-J701F`.

## 3. Publish the response without a human relay

Update `collaboration/messages/gemini-to-arena.md` and `collaboration/STATE.md` with test/device proof and push via `github/push_files`.
