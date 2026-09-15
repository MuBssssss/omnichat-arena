# OmniChat Arena agent contract

This repository is shared by the Arena architect agent and the Gemini build/debug agent. The
repository is the collaboration network: do not make the human copy source files or terminal
output between agents.

## Read first

1. `collaboration/PROTOCOL.md`
2. `collaboration/STATE.md`
3. The current mailbox addressed to you in `collaboration/messages/`
4. `collaboration/next_commands_for_gemini.md` when working as Gemini

## Non-negotiable rules

- Never commit, paste, print, or log API keys, cookies, session IDs, bearer tokens, or device
  credentials. Redact them as `***` in reports. Secrets belong only in the app's encrypted
  store or the agent's local secret manager.
- Keep the $0-only and native Android constraints in `HANDOFF_TO_GEMINI.md`.
- Preserve the latest mailbox message and update `collaboration/STATE.md` in the same published
  change as the response. Git history is the audit trail; the mailbox files are the current
  state.
- A finished task must leave a machine-readable next step. Do not ask the human to relay a
  routine status update or command.

## Branches and publishing

- The Arena agent works only on `arena/01a0a4da-omnichat-arena`.
- Gemini may use its own working branch, but must publish its mailbox response where the Arena
  agent can read it. The simplest route is the GitHub MCP file-update tool targeting the Arena
  branch for **mailbox files only**; never overwrite the Arena agent's source changes.
- Publish code, tests, docs, and the mailbox response atomically when possible. If using the
  GitHub MCP server, prefer a single `github/push_files` call, then verify with
  `github/list_commits`.

## MCP expectation for Gemini

Use the available MCP servers instead of asking the human to perform routine inspection:

- GitHub MCP: inspect the repository and branch, read the current mailbox, publish batched
  changes, and verify the resulting commit. Useful tools include `github/get_me`, file-content
  reads, `github/push_files` or `github/create_or_update_file`, and `github/list_commits`.
- `mobile-mcp`: when Android behavior is relevant, list devices, launch the app, inspect the
  semantic tree, interact with it, and take screenshots. Record only non-sensitive proof.

MCP use does not relax the secret rule. Never send a secret to an MCP tool or include it in a
screenshot, issue, commit, or mailbox message.
