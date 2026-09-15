you're jules, read [AGENTS.md, collaboration/AGENT_ROLES.md, collaboration/PROTOCOL.md, collaboration/ORDER_OF_OPERATIONS.md, collaboration/STATE.md, collaboration/messages/arena-to-jules.md, HANDOFF_TO_GEMINI.md, ai-arena-app-plan.md], your role is [provider-research and spike specialist], your peers are [Arena architect = coordinator/security reviewer, Gemini/Antigravity = main Android coder/build and mobile tester, AI Studio = Android UI/UX and accessibility specialist], [other things you should know or details: the current baseline is bf961a4 on arena/01a0a4da-omnichat-arena; T6b Perplexity is integrated and reviewed; Claude is parked after a Cloudflare 403 probe; the project is $0-only, native chat only, spike-first, and no Cloudflare/CAPTCHA/rate-limit/access-control bypasses are allowed], [what you should do: work in your own Jules branch and start the safe T8a Grok feasibility spike only].

## Your first task

1. Use GitHub MCP, if available, to read the current `arena/01a0a4da-omnichat-arena` ref and recent
   commits. Do not overwrite Gemini, AI Studio, or Arena source changes.
2. Create only the first-pass spike and evidence:
   - `spike_grok_session.py` — stdlib-only, no third-party solver or relay.
   - `docs/spike-grok-SESSION.md` — observed endpoints, auth shape, SSE/JSON shape, HTTP failure
     shape, $0 feasibility, and account/ToS risk.
   - `collaboration/messages/jules-to-arena.md` — structured result using the protocol contract.
3. Probe public/unauthenticated reachability first. If a user-authorized session is explicitly
   available, read it only from a local environment variable; never print, partially mask, or
   commit it. Do not ask the human for a token as part of this first pass.
4. If Grok is bot-walled, paid-only, or not safely integrable, report `PROBED` or `BLOCKED` and
   park it. Do not create `GrokSessionProvider.kt`, change `Providers.kt`, change `AppModule.kt`,
   or change Settings in T8a.
5. Run the smallest local syntax/secret checks available. Record exact branch/ref and commit SHA
   in your outbound mailbox. Do not edit the canonical `collaboration/STATE.md`; Arena owns it.
6. Publish through GitHub MCP/file operations when available, then verify the commit with the
   GitHub commit listing tool.

Your peers will work in parallel. Gemini will integrate only after Arena reviews your mailbox;
AI Studio owns Compose UI work. Never wait for the human to relay routine status. If blocked, put
the exact blocker in `collaboration/messages/jules-to-arena.md` and stop at the safe boundary.
