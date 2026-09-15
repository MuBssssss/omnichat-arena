# Multi-agent roster and ownership

The project now has four active AI contributors. The repository mailbox is the transport; the
human should not relay routine code, logs, or status between them.

| Agent | Role | Owns | Must not own concurrently |
|---|---|---|---|
| Arena architect | Architecture, security review, task coordinator | `collaboration/`, architecture docs, review patches, final acceptance | Gemini's device-only work or worker spike branches unless explicitly assigned |
| Gemini / Antigravity | Main coder, integrator, build and hardware tester | Android integration, Gradle, DI, provider wiring, APK builds, `mobile-mcp`, final merges | Jules spike files or AI Studio UI files while those tasks are in flight |
| Jules (`jules.google.com/session`) | Provider research and spike specialist | `spike_*.py`, `docs/spike-*-SESSION.md`, protocol evidence, one provider investigation at a time | Android provider code, Settings/UI, shared core/DI |
| AI Studio (`aistudio.google.com/apps`) | Android UI/UX, accessibility, and interaction-quality specialist | Compose UI package, UI audit docs, UI-focused tests/fixtures | Provider networking, SecretStore/DI, Gradle, release integration |

## Current baseline

- Current branch: `arena/01a0a4da-omnichat-arena`
- Current published baseline: `bf961a4` (T7a Claude probe completed; Claude is parked after a
  Cloudflare challenge observation).
- T6b Perplexity provider is integrated, reviewed, and build/device-tested by Gemini. Do not
  claim authenticated Perplexity E2E without a real user-authorized session test.
- Claude and any other unofficial session provider remain opt-in, $0-only, spike-first, and
  parkable. No one may bypass Cloudflare, CAPTCHA, rate limits, access controls, or account
  restrictions.

## Shared rules for every agent

1. Read `AGENTS.md`, this file, `collaboration/PROTOCOL.md`, `collaboration/ORDER_OF_OPERATIONS.md`,
   `collaboration/STATE.md`, and your addressed mailbox before editing.
2. Use a separate working branch or an isolated file set. Never overwrite another agent's source
   changes just to publish a mailbox update.
3. Use GitHub MCP/file operations when available to read the current ref, publish a coherent
   batch, and verify the commit. Put the exact branch/ref and SHA in your outbound mailbox.
4. Use only local environment/secure storage for credentials. Never print, partially mask, commit,
   screenshot, or send a token, cookie, account identifier, private prompt, or response body.
5. Publish a structured result to your own `*-to-arena.md` mailbox. The Arena agent alone updates
   the canonical `collaboration/STATE.md` cursor so simultaneous workers do not overwrite it.
6. Do not silently change scope. If blocked, report `BLOCKED` with the first reproducible blocker
   and park the task safely.

## Communication addresses

- Arena -> Gemini: `collaboration/messages/arena-to-gemini.md`
- Gemini -> Arena: `collaboration/messages/gemini-to-arena.md`
- Arena -> Jules: `collaboration/messages/arena-to-jules.md`
- Jules -> Arena: `collaboration/messages/jules-to-arena.md`
- Arena -> AI Studio: `collaboration/messages/arena-to-aistudio.md`
- AI Studio -> Arena: `collaboration/messages/aistudio-to-arena.md`

Workers may add a peer note to their outbound mailbox, but the coordinator mailbox is the source
of truth for merges and acceptance.
