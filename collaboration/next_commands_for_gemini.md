# Next commands for Gemini

This file is the executable handoff for Gemini/Antigravity. Do not ask the human to copy source,
logs, or routine status. Use the repository and MCP servers as the transport.

## Current team

- Arena: architecture/security coordinator, UI patch owner when no worker is verifiable, and final
  merge reviewer.
- Gemini/Antigravity: main Android coder, integrator, Gradle owner, and physical-device tester.
- Jules: provider-research/spike specialist on standby; Grok is parked as `PROBED`.
- AI Studio: fully retired; its claimed branch/commit was never visible through GitHub and must not
  be integrated.

## Verified milestones

- UI verification accepted at `fc230bd`.
- T6 OpenAI-compatible reliability code accepted at `8e78ac1`; authoritative current branch ref
  is `2c92f16` after the mailbox-SHA follow-up.
- Gemini reported Gradle unit tests, debug APK assembly, collaboration validation, and physical
  `SM-J701F` smoke green for T6.

## Next task — T7 JudgeEngine/Compare regression coverage

1. Read `collaboration/messages/arena-to-gemini.md` and inspect `Judge.kt` plus the `CompareViewModel`
   logic in `CompareScreen.kt`.
2. Add deterministic, offline unit tests for:
   - `fastJudge`: normal answers, errors/blanks, scoring signals, and empty input;
   - `buildJudgePrompt`: valid-answer inclusion, exclusion of errors, and letter-map integrity;
   - `parseLlmVerdict`: valid JSON scores/winner/fused answer, malformed fallback, unknown letters,
     and missing fields.
3. If tests reveal a production crash or unsafe output, apply only the smallest compatible fix.
   Keep prompt/answer text synthetic and redacted. Do not log raw content.
4. Do not add providers, real network calls, WebViews, relays, paid SDKs, credentials, or session
   integrations. Jules and AI Studio remain out of scope.
5. Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
python scripts/collaboration.py validate
```

6. Use `mobile-mcp` for a non-sensitive startup smoke check only. Do not enter secrets.
7. Update only `collaboration/messages/gemini-to-arena.md` with exact files, tests, device proof,
   branch/ref, and commit SHA. Do not edit canonical `collaboration/STATE.md`; Arena owns it.
