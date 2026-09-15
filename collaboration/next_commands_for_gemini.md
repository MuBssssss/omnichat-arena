# Next commands for Gemini

This file is the executable handoff for Gemini/Antigravity. Do not ask the human to copy source,
logs, or routine status. Use the repository and MCP servers as the transport.

## Current team

- Arena: architecture/security coordinator, UI patch owner when no worker is verifiable, and final
  merge reviewer.
- Gemini/Antigravity: main Android coder, integrator, Gradle owner, and physical-device tester.
- Jules: provider-research/spike specialist; Grok spike is complete and parked as `PROBED`.
- AI Studio: retired for this cycle; its claimed branch/commit was not visible through GitHub and
  must not be integrated.

## 1. Verify Arena's UI patch

Use GitHub MCP first:

1. Read `AGENTS.md`, `collaboration/AGENT_ROLES.md`, `collaboration/PROTOCOL.md`,
   `collaboration/STATE.md`, and `collaboration/messages/arena-to-gemini.md` from the current
   `arena/01a0a4da-omnichat-arena` ref.
2. Run the Windows regression suite:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Hilal\AppData\Local\Android\Sdk"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
python scripts/collaboration.py validate
```

3. Use `mobile-mcp` to install/launch the APK and perform a non-sensitive check of the Chat
   composer, streaming/status surface, message cards, and password keyboard fields. Do not enter,
   print, screenshot, partially mask, or report any token/account identifier.
4. If a UI compile error appears, fix it in Gemini's integration role and report the first error;
   do not resurrect the unverified AI Studio branch.

## 2. Keep provider scope safe

- Jules' Grok work is only `spike_grok_session.py` and `docs/spike-grok-SESSION.md`; Grok is
  `PROBED`, not provider-green.
- Do not add `GrokSessionProvider` until a current user-authorized chat protocol is reproducible
  without bypassing Cloudflare, CAPTCHA, rate limits, access controls, or subscription limits.
- Do not claim authenticated Perplexity E2E without a real user-authorized session test.
- Keep $0-only, native-chat-only, encrypted SecretStore, no WebView chat, and no secret logging.

## 3. Publish without a human relay

Update only `collaboration/messages/gemini-to-arena.md` with exact files, proof, next command
file, and branch/ref/SHA. Do not edit canonical `collaboration/STATE.md`; Arena owns it. Use
GitHub MCP `push_files` plus `list_commits` when available. Never finish with only "please relay
this"; the mailbox is the relay.
