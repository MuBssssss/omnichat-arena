you're aistudio, read [AGENTS.md, collaboration/AGENT_ROLES.md, collaboration/PROTOCOL.md, collaboration/ORDER_OF_OPERATIONS.md, collaboration/STATE.md, collaboration/messages/arena-to-aistudio.md, HANDOFF_TO_GEMINI.md, README.md], your role is [Android UI/UX, accessibility, and interaction-quality specialist], your peers are [Arena architect = coordinator/security reviewer, Gemini/Antigravity = main Android coder/build and mobile tester, Jules = provider-research and spike specialist], [other things you should know or details: the current baseline is bf961a4 on arena/01a0a4da-omnichat-arena; the app is native Jetpack Compose, $0-only, and must not use a WebView for chat; provider networking, SecretStore, DI, and Gradle are owned by Gemini], [what you should do: work in your own AI-Studio branch and perform the first UI/accessibility quality slice only].

## Your first task

1. Use GitHub MCP, if available, to read the current `arena/01a0a4da-omnichat-arena` ref and recent
   commits. Do not overwrite Jules, Gemini, or Arena source changes.
2. Inspect the existing Compose surfaces:
   - `app/src/main/java/com/omnichat/arena/ui/ChatScreen.kt`
   - `app/src/main/java/com/omnichat/arena/ui/CompareScreen.kt`
   - `app/src/main/java/com/omnichat/arena/ui/SettingsScreen.kt`
3. Create `docs/ui-audit.md` with concrete findings covering 720p phones, touch targets,
   scrolling, keyboard behavior, content descriptions, error/status announcements, password
   field safety, and the minimal-interaction goal.
4. If you can safely build/test, implement only a small, clearly scoped UI/accessibility slice in
   the UI package and add UI-focused tests/fixtures if the project supports them. Do not modify
   provider networking, `SecretStore`, `AppModule.kt`, `Providers.kt`, Gradle files, or session
   protocols in this first pass.
5. Keep the UI native Compose. Do not add WebView chat, analytics, a paid SDK, a remote relay, or
   hard-coded credentials. Do not print or screenshot account data or token fields.
6. Run the smallest validation available, record exact branch/ref and commit SHA, and publish a
   structured result to `collaboration/messages/aistudio-to-arena.md`. Do not edit canonical
   `collaboration/STATE.md`; Arena owns it.
7. Publish through GitHub MCP/file operations when available and verify the published commit.

Gemini will integrate only after Arena reviews your mailbox, while Jules works on a provider
spike. Never wait for the human to relay routine status. If you cannot safely modify UI, submit
the audit and mark the task `BLOCKED` with the first reproducible reason.
