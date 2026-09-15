# Next commands for Gemini

This file is the executable handoff for the next Gemini turn. Do not ask the human to copy
source, logs, or routine status. Use the repository and MCP servers as the transport.

## Status: T7a Claude Probe Complete — Awaiting Arena Review

T7a Claude web-session probe completed:
- `spike_claude_session.py` ran live: `https://claude.ai` returns HTTP 403 with active Cloudflare Bot Management (`cf-ray`).
- Documented in `docs/spike-claude-SESSION.md` with recommendation to park Claude alongside DeepSeek and Duck.ai.
- T6b regression tests and Android APK build remain 100% green and verified on `SM-J701F`.

## 1. Synchronize and read the mailbox

Use GitHub MCP first:

1. Read `AGENTS.md`, `collaboration/PROTOCOL.md`, `collaboration/STATE.md`, and
   `collaboration/messages/arena-to-gemini.md` from the published ref
   `arena/01a0a4da-omnichat-arena`.
2. Inspect `HANDOFF_TO_GEMINI.md`, `ai-arena-app-plan.md`, and the files named in the mailbox.
3. Keep all credentials local to the user's secure store. Do not put a real key, cookie, session
   ID, bearer token, account email, or device credential in a command, MCP argument, screenshot,
   log, or commit.

## 2. Next Sprint Target

Awaiting Arena architect review of T7a findings to decide between:
- **T8 Grok Spike:** Probe xAI web-session or free tier feasibility.
- **Arena Multi-Contender Engine Polish:** Advance judge scoring and 3-way/N-way comparison capabilities.
- **Provider Park UI Updates:** Formally mark Claude as parked in Settings with Cloudflare bot-wall disclaimer.

## 3. Publish the response without a human relay

Update `collaboration/messages/gemini-to-arena.md` and `collaboration/STATE.md` with test/device proof and push via `github/push_files`.
