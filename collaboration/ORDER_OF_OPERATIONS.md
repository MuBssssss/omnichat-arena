# Parallel work order

For the exact human copy-paste messages, use
`collaboration/HUMAN_LAUNCH_GUIDE.md`. This file is the short operational version.

## Phase 1 — Gemini first, alone

1. Open Gemini/Antigravity.
2. Paste the Gemini message from `collaboration/HUMAN_LAUNCH_GUIDE.md`.
3. Gemini reads the current ref, runs Gradle tests/build, runs the collaboration validator, and
   performs one small non-sensitive `mobile-mcp` smoke check.
4. Gemini publishes the baseline result in `collaboration/messages/gemini-to-arena.md` and stops.

Do not start the worker agents until this baseline result is published. This makes the starting
commit and build status unambiguous.

## Phase 2 — Jules and AI Studio together

Start both sessions at the same time after Phase 1:

- Jules: open `https://jules.google.com/session`, paste the Jules box from the human guide, and
  work only on the T8a Grok feasibility spike plus docs.
- AI Studio: open `https://aistudio.google.com/apps`, paste the AI Studio box from the human guide,
  and work only on the Compose UI/accessibility audit and optional small UI slice.

They use separate branches/file ownership and publish independent mailboxes. Neither waits for the
other. The human does not relay their results.

## Phase 3 — Arena review

Arena reads `jules-to-arena.md` and `aistudio-to-arena.md`, checks scope, secrets, $0 compliance,
tests, and conflicts, then updates the canonical `collaboration/STATE.md` cursor.

## Phase 4 — Gemini integration

Only after Arena approval, Gemini integrates approved worker commits, resolves conflicts, runs the
full Gradle checks, installs/tests the APK with `mobile-mcp`, and publishes the integration report.

## Ownership guardrails

- Gemini owns Android integration, Gradle, DI/provider wiring, builds, and device testing.
- Jules owns spike scripts and spike docs only during the first pass.
- AI Studio owns Compose UI, accessibility, and UI audit files only during the first pass.
- Arena owns architecture review, coordination, and `collaboration/STATE.md`.
