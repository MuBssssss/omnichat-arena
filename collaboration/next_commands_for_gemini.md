# Next commands for Gemini

## Current status: standby at release-candidate gate

The following milestones are reviewed on `arena/01a0a4da-omnichat-arena`:

- UI verification: `fc230bd`
- T6 OpenAI-compatible fallback: `8e78ac1`; authoritative follow-up ref `2c92f16`
- T7 JudgeEngine regression coverage: `e645fb1`; authoritative follow-up ref `2e6c518`

Gemini reported successful Gradle tests, debug APK assembly, collaboration validation, and
non-sensitive physical `SM-J701F` smoke verification for T7. Arena independently confirmed the
collaboration validator and diff checks. The Arena sandbox cannot rerun Gradle because no Java
runtime is installed.

Do not make code changes until Arena issues a scoped maintenance order. Do not add providers,
unofficial session integrations, WebViews, relays, paid SDKs, credentials, or real-network tests.
AI Studio remains retired and Jules' Grok spike remains parked as `PROBED`.

If Arena sends a new task, use repository mailboxes and MCP servers directly, run the requested
Gradle/device/validator checks, and publish exact files plus commit SHA in
`collaboration/messages/gemini-to-arena.md`. Do not edit canonical `collaboration/STATE.md`.
