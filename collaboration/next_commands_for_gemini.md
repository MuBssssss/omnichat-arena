# Next commands for Gemini

This file is the executable handoff for the next Gemini turn. Do not ask the human to copy
source, logs, or routine status. Use the repository and MCP servers as the transport.

## 1. Synchronize and read the mailbox

Use GitHub MCP first:

1. Read `AGENTS.md`, `collaboration/PROTOCOL.md`, `collaboration/STATE.md`, and
   `collaboration/messages/arena-to-gemini.md` from the published ref
   `arena/01a0a4da-omnichat-arena`.
2. Use `github/list_commits` to verify the latest commit/ref before editing.
3. Inspect `HANDOFF_TO_GEMINI.md`, `ai-arena-app-plan.md`, and the files named in the mailbox.
4. Keep all credentials local to the user's secure store. Do not put a real key, cookie,
   session ID, or device credential in a command, MCP argument, screenshot, log, or commit.

If a local checkout is needed, fetch the ref without switching the Arena branch:

```powershell
git fetch origin --prune
git log --oneline --decorate -10 origin/arena/01a0a4da-omnichat-arena
```

## 2. Continue the next engineering task

Resume the first unchecked task in `HANDOFF_TO_GEMINI.md` rather than inventing a new scope.
The current cursor is the Perplexity web-session spike/hardening track after T4.2:

1. Spike first in a small, stdlib-only script; do not add an Android provider before the HTTP
   behavior and failure shape are documented.
2. Keep the $0-only rule, native chat UI rule, session-storage rule, and account/ToS warnings.
3. If the spike is blocked, document the evidence and park it cleanly; do not fabricate a live
   integration.
4. If the spike passes, implement only the isolated provider changes needed for that task and
   keep the project compiling.

Use the available `mobile-mcp` tools for any Android acceptance check: list the device, launch
OmniChat, inspect the screen, perform the smallest relevant interaction, and take a proof
screenshot. Redact sensitive content before reporting it.

## 3. Publish the response without a human relay

Update these files in the same published change:

- `collaboration/messages/gemini-to-arena.md` — use the full message contract in
  `collaboration/PROTOCOL.md`.
- `collaboration/STATE.md` — set `Last message ID`, status, commit/ref, and next owner.
- `collaboration/next_commands_for_gemini.md` — replace the completed step with the next exact
  commands, or leave it unchanged if the next task is already explicit.

Publish implementation and mailbox changes with one `github/push_files` call when possible, then
use `github/list_commits` to confirm the commit. If the MCP server cannot target the Arena ref,
publish the response on your working branch and record its exact branch and commit in
`collaboration/STATE.md`.

The response must end with one of these outcomes:

- `DONE`: include files, proof, and the next command file.
- `BLOCKED`: include the first reproducible blocker and the one decision/action needed.

Never finish with only "please relay this"; the mailbox is the relay.
