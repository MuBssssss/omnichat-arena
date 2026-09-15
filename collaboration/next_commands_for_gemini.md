# Next commands for Gemini

This file is the executable handoff for Gemini/Antigravity. Do not ask the human to copy source,
logs, or routine status. Use the repository and MCP servers as the transport.

## Current team and ownership

- Arena: architecture/security coordinator and merge reviewer; owns `collaboration/STATE.md`.
- Gemini/Antigravity: main Android coder, integrator, Gradle owner, and physical-device tester.
- Jules: provider-research/spike specialist; owns only `spike_*.py` and spike docs in the first pass.
- AI Studio: Android UI/UX/accessibility specialist; owns Compose UI/audit files in the first pass.

Read `collaboration/AGENT_ROLES.md`, `collaboration/ORDER_OF_OPERATIONS.md`, and
`collaboration/prompts/GEMINI_ANTIGRAVITY.md` before editing.

## 1. Baseline now

Use GitHub MCP first:

1. Read the current `arena/01a0a4da-omnichat-arena` ref, recent commits, all three incoming task
   mailboxes, and the role/prompt files.
2. Run the Windows regression suite on the current baseline:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Hilal\AppData\Local\Android\Sdk"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
python scripts/collaboration.py validate
```

3. Use `mobile-mcp` for the smallest non-sensitive smoke check. Do not enter, screenshot, print,
   or report any session token/account identifier.
4. Publish the result in `collaboration/messages/gemini-to-arena.md` with branch/ref and SHA. Do
   not edit canonical `collaboration/STATE.md` while Jules and AI Studio are working.

## 2. Parallel lanes

While the baseline runs or after it is green:

- Jules is doing the T8a Grok feasibility spike in `spike_grok_session.py` and
  `docs/spike-grok-SESSION.md`. Do not edit those files.
- AI Studio is doing a Compose UI/accessibility audit and possibly a small UI slice. Do not edit
  its UI/audit files unless Arena assigns a conflict.
- Gemini owns build failures, Android integration, DI/provider wiring, APK installation, and
  `mobile-mcp` acceptance.

Wait for Arena's approval mail before integrating worker branches. Review each diff for secrets,
$0 compliance, native UI rules, tests, and ownership. Then integrate, run Gradle, and test the
approved changes on the device.

## 3. Safety constraints

- No paid APIs/SDKs, WebView chat, Cloudflare/CAPTCHA/rate-limit/access-control bypasses, hidden
  relay, or automatic provider retries that could trigger account protection.
- Session values are local-only and encrypted. Never print, partially mask, commit, screenshot,
  or send them to an MCP tool.
- Keep a provider `PROBED`/`PARKED` if authenticated behavior is not actually proven.

## 4. Publish without a human relay

Update only `collaboration/messages/gemini-to-arena.md` with the protocol headings, exact files,
proof, next command file, and branch/ref/SHA. Use GitHub MCP `push_files` plus `list_commits` when
available. Never finish with only "please relay this"; the mailbox is the relay.
