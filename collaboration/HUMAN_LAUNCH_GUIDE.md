# Human launch guide — current order

AI Studio is retired for this cycle because its claimed branch/commit could not be verified through
GitHub. Arena has taken the small UI/accessibility work directly. You only need Gemini and Jules.
Do not copy code, logs, tokens, or worker results between agents.

## Phase 1 — Gemini starts first

Open Gemini/Antigravity and paste:

```text
You're Gemini/Antigravity, the main Android coder, integrator, build owner, and physical-device tester for OmniChat Arena.

Read:
- AGENTS.md
- collaboration/AGENT_ROLES.md
- collaboration/PROTOCOL.md
- collaboration/ORDER_OF_OPERATIONS.md
- collaboration/STATE.md
- collaboration/prompts/GEMINI_ANTIGRAVITY.md
- collaboration/messages/arena-to-gemini.md
- HANDOFF_TO_GEMINI.md

Run the baseline verification:
1. Use GitHub MCP to inspect the current arena/01a0a4da-omnichat-arena ref.
2. Run testDebugUnitTest and assembleDebug.
3. Run python scripts/collaboration.py validate.
4. Use mobile-mcp for one small non-sensitive smoke check.
5. Never enter, print, screenshot, partially mask, or commit tokens, cookies, account identifiers, private prompts, or response bodies.
6. Do not add a Grok provider and do not wait for AI Studio; AI Studio is retired for this cycle.
7. Publish the report only in collaboration/messages/gemini-to-arena.md with exact branch/ref and commit SHA. Arena owns collaboration/STATE.md.

After the baseline report, stop and wait for Arena's integration order.
```

## Phase 2 — Jules works on provider research

Jules' Grok spike is already completed and reviewed. If a future Jules task is assigned, open
https://jules.google.com/session and give it the current `collaboration/prompts/JULES.md`. Jules
must remain on spike scripts/docs and must not implement Android provider code without Arena
approval.

## Phase 3 — Arena reviews and patches

Arena reviews worker evidence and directly handles small UI/accessibility patches when a UI worker
is unavailable. Current Arena UI work is documented in `docs/ui-audit.md`.

## Phase 4 — Gemini final verification

Gemini pulls the latest Arena branch, reruns Gradle tests/build, runs the collaboration validator,
installs the APK, and performs the non-sensitive `mobile-mcp` acceptance check. Gemini publishes the
final report in `collaboration/messages/gemini-to-arena.md`.
