# Current work order

For the human copy-paste sequence, see `collaboration/HUMAN_LAUNCH_GUIDE.md`. The current
AI-Studio lane is retired because its claimed branch/commit could not be verified through GitHub.
Arena has taken the small UI slice directly.

## Phase 1 — Gemini baseline

1. Gemini reads the current ref and runs the Gradle tests/build, collaboration validator, and one
   small non-sensitive `mobile-mcp` smoke check.
2. Gemini publishes the result in `collaboration/messages/gemini-to-arena.md`.
3. Arena reviews the baseline before integration.

## Phase 2 — Jules spike

Jules' first-pass Grok feasibility spike is complete and approved only as `PROBED`:

- `spike_grok_session.py`
- `docs/spike-grok-SESSION.md`

No Grok Android provider is approved. Future provider work must still be spike-first.

## Phase 3 — Arena UI review/patch

Arena directly implemented the small UI/accessibility slice described in `docs/ui-audit.md`:

- Chat auto-scroll during new/streaming content.
- IME Send behavior for the prompt field.
- TalkBack polite live region for generation state.
- Differentiated user/model/error message cards.
- Password keyboard and autocorrect suppression for all credential fields.

## Phase 4 — Gemini integration verification

Gemini must pull the latest Arena branch, run `testDebugUnitTest`, `assembleDebug`, the collaboration
validator, and a non-sensitive `mobile-mcp` check. Only Gemini owns the final Android build/device
acceptance.

## Ownership guardrails

- Gemini owns Android integration, Gradle, DI/provider wiring, builds, and device testing.
- Jules owns spike scripts and spike docs only during a spike.
- Arena owns architecture review, coordination, UI patches, and `collaboration/STATE.md`.
- AI Studio is retired for this cycle; do not wait for or integrate its unverified commit.
