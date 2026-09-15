# Parallel work order

This is the low-interaction launch order for the four-agent team. Steps 1–3 can start
simultaneously after everyone reads the baseline. Step 4 begins only when worker mailboxes have
reported results.

## 1. Start Gemini / Antigravity — main coder, builder, tester

Send `collaboration/prompts/GEMINI_ANTIGRAVITY.md`.

Gemini should first:

1. Read the current branch, roster, protocol, and all three incoming task mailboxes.
2. Run the baseline Gradle tests/build and the smallest `mobile-mcp` smoke check.
3. Keep the integration branch buildable while waiting for the workers.
4. Avoid editing Jules-owned spike files or AI Studio-owned UI files unless a conflict is
   explicitly assigned by Arena.

## 2. Start Jules — provider research and spike specialist (parallel with AI Studio)

Send `collaboration/prompts/JULES.md`.

Jules should first create only:

- `spike_grok_session.py`
- `docs/spike-grok-SESSION.md`
- `collaboration/messages/jules-to-arena.md`

The first task is a safe T8a Grok feasibility probe. A blocked/bot-walled result is useful and
should be parked; no Android provider is allowed in this first pass.

## 3. Start AI Studio — Android UI/UX and accessibility specialist (parallel with Jules)

Send `collaboration/prompts/AI_STUDIO.md`.

AI Studio should first inspect and, if possible, implement a small UI-quality slice in its own
branch:

- `app/src/main/java/com/omnichat/arena/ui/ChatScreen.kt`
- `app/src/main/java/com/omnichat/arena/ui/CompareScreen.kt`
- `app/src/main/java/com/omnichat/arena/ui/SettingsScreen.kt`
- `docs/ui-audit.md` and UI-focused tests/fixtures as needed

It must not modify networking, SecretStore, DI, provider code, or Gradle in this first pass.

## 4. Arena review and Gemini integration

After Jules and AI Studio publish their mailbox results:

1. Arena reviews each diff for scope, secret hygiene, $0 compliance, and merge conflicts.
2. Arena updates `collaboration/STATE.md` with separate worker statuses and approval/parking
   decisions.
3. Gemini reads the approved worker commits, integrates them, resolves conflicts, runs Gradle,
   and uses `mobile-mcp` for device acceptance.
4. Gemini publishes the integrated result; Arena performs final review and assigns the next
   parallel batch.

## Human interaction required

The human only needs to open the three agent sessions and paste the respective prompt files. They
should not copy code, cookies, logs, or tokens. Any provider login/session test requiring the
account owner must be explicitly called out as a single opt-in action and never automated through
Cloudflare/CAPTCHA bypasses.
