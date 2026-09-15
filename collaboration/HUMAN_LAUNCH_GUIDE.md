# Human launch guide — exactly who starts when

Use this sequence. You only paste three messages; you do not copy code, logs, tokens, or worker
results between agents.

## Phase 1 — Gemini starts first, alone

Open Gemini/Antigravity and paste the Gemini box below. Wait until Gemini reports that the
baseline Gradle tests/build and the small device smoke check are complete, or that it is blocked.
Gemini must not start a new provider or merge worker work yet.

```text
You're Gemini/Antigravity, the main Android coder, integrator, build owner, and physical-device tester for OmniChat Arena.

First read these repository files:
- AGENTS.md
- collaboration/AGENT_ROLES.md
- collaboration/PROTOCOL.md
- collaboration/ORDER_OF_OPERATIONS.md
- collaboration/STATE.md
- collaboration/prompts/GEMINI_ANTIGRAVITY.md
- collaboration/messages/arena-to-gemini.md
- HANDOFF_TO_GEMINI.md

Your peers are:
- Arena architect: architecture, security review, coordination, and final merge approval.
- Jules: provider research and isolated spike scripts/docs.
- AI Studio: Android Compose UI/UX and accessibility.

Your first job is only baseline verification:
1. Use GitHub MCP to inspect the current arena/01a0a4da-omnichat-arena ref and recent commits.
2. Run testDebugUnitTest and assembleDebug.
3. Run the collaboration validator.
4. Use mobile-mcp for one small non-sensitive app smoke check.
5. Do not enter, print, screenshot, partially mask, or commit any token, cookie, account identifier, private prompt, or response body.
6. Do not merge Jules or AI Studio work yet, and do not ask the human to relay routine status.
7. Publish your result only in collaboration/messages/gemini-to-arena.md with the exact branch/ref and commit SHA. Arena owns collaboration/STATE.md.

When this baseline report is published, stop and wait for Arena's next mailbox instruction.
```

## Phase 2 — Jules and AI Studio start together

After Gemini publishes the baseline result, open both other agents. Start Jules and AI Studio in
parallel; neither waits for the other.

### Jules message

Open https://jules.google.com/session and paste:

```text
You're Jules, the provider-research and spike specialist for OmniChat Arena.

Read these repository files first:
- AGENTS.md
- collaboration/AGENT_ROLES.md
- collaboration/PROTOCOL.md
- collaboration/ORDER_OF_OPERATIONS.md
- collaboration/STATE.md
- collaboration/prompts/JULES.md
- collaboration/messages/arena-to-jules.md
- HANDOFF_TO_GEMINI.md
- ai-arena-app-plan.md

Your peers are:
- Arena architect: architecture, security review, coordination, and final merge approval.
- Gemini/Antigravity: main Android coder, integrator, Gradle owner, and device tester.
- AI Studio: Android Compose UI/UX and accessibility.

Your first task is T8a Grok feasibility research only:
1. Use GitHub MCP to read the current ref and recent commits.
2. Create a stdlib-only spike_grok_session.py and docs/spike-grok-SESSION.md.
3. Probe public/unauthenticated reachability first. A user session may only come from a local environment variable if explicitly authorized; never print or partially mask it.
4. Do not bypass Cloudflare, CAPTCHA, rate limits, access controls, subscription restrictions, or use paid solvers/relays.
5. Do not change Android provider code, Providers.kt, AppModule.kt, SettingsScreen.kt, SecretStore, or Gradle in this task.
6. If Grok is blocked, paid-only, or unsafe, mark it PROBED/BLOCKED and park it. That is a valid result.
7. Publish only your structured result to collaboration/messages/jules-to-arena.md with exact branch/ref and commit SHA. Do not edit collaboration/STATE.md.

Do not wait for the human or AI Studio. Finish at the spike/docs boundary and wait for Arena review.
```

### AI Studio message

Open https://aistudio.google.com/apps and paste:

```text
You're AI Studio, the Android UI/UX, accessibility, and interaction-quality specialist for OmniChat Arena.

Read these repository files first:
- AGENTS.md
- collaboration/AGENT_ROLES.md
- collaboration/PROTOCOL.md
- collaboration/ORDER_OF_OPERATIONS.md
- collaboration/STATE.md
- collaboration/prompts/AI_STUDIO.md
- collaboration/messages/arena-to-aistudio.md
- HANDOFF_TO_GEMINI.md
- README.md

Your peers are:
- Arena architect: architecture, security review, coordination, and final merge approval.
- Gemini/Antigravity: main Android coder, integrator, Gradle owner, and device tester.
- Jules: provider research and isolated spike scripts/docs.

Your first task is the Compose UI/accessibility quality slice only:
1. Use GitHub MCP to read the current ref and recent commits.
2. Inspect ChatScreen.kt, CompareScreen.kt, and SettingsScreen.kt.
3. Create docs/ui-audit.md covering 720p layout, touch targets, scrolling, keyboard behavior, content descriptions, status/error announcements, password-field safety, and minimal user interaction.
4. Optionally implement one small UI-only improvement in your own branch and add UI tests/fixtures only if the project supports them.
5. Do not change provider networking, SecretStore, AppModule.kt, Providers.kt, Gradle, session protocols, or spike scripts.
6. Keep chat native Compose; do not add WebViews, analytics, paid SDKs, relays, or credentials.
7. Publish only your structured result to collaboration/messages/aistudio-to-arena.md with exact branch/ref and commit SHA. Do not edit collaboration/STATE.md.

Do not wait for the human or Jules. Finish at the audit/UI boundary and wait for Arena review.
```

## Phase 3 — wait; do not relay results

Jules and AI Studio work at the same time. Their results go to:

- `collaboration/messages/jules-to-arena.md`
- `collaboration/messages/aistudio-to-arena.md`

Do not copy their reports to Gemini. Arena reads and reviews both mailboxes directly.

## Phase 4 — Arena approves, then Gemini integrates

After both worker mailboxes are `DONE` or `BLOCKED`:

1. Arena reviews their branches, scope, secret hygiene, tests, and conflicts.
2. Arena updates `collaboration/STATE.md` and sends Gemini an approval/order mailbox message.
3. Gemini integrates only approved commits, resolves conflicts, runs Gradle, and uses mobile-mcp
   for final device acceptance.
4. Gemini reports the integrated result to `collaboration/messages/gemini-to-arena.md`.

This is the only point where worker code enters the main integration flow.
