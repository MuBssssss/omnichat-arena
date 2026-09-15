# OmniChat Arena agent contract

This repository is a shared collaboration network for the Arena architect, Gemini/Antigravity,
and Jules. AI Studio is archived/retired for this cycle because its claimed branch was not
verifiable through GitHub. Do not make the human copy source files, logs, or routine status between
agents. Read `collaboration/AGENT_ROLES.md` for ownership before editing.

## Read first

1. `AGENTS.md`
2. `collaboration/AGENT_ROLES.md`
3. `collaboration/PROTOCOL.md`
4. `collaboration/ORDER_OF_OPERATIONS.md`
5. `collaboration/STATE.md`
6. The current mailbox addressed to you in `collaboration/messages/`
7. Your prompt in `collaboration/prompts/`

## Non-negotiable rules

- Never commit, paste, print, partially mask, or log API keys, cookies, session IDs, bearer
  tokens, account identifiers, private prompts, response bodies, or device credentials. Secrets
  belong only in the app's encrypted store or an agent's local secret manager.
- Keep the $0-only, native Android, spike-first, and no-bypass constraints in
  `HANDOFF_TO_GEMINI.md` and `collaboration/AGENT_ROLES.md`.
- Respect file ownership. Workers use their own branch/file set; Gemini integrates only after
  Arena review. Do not overwrite another agent's source changes to publish a mailbox update.
- Workers publish their own structured `*-to-arena.md` mailbox and exact branch/ref/SHA. The Arena
  architect alone owns the canonical `collaboration/STATE.md` cursor during parallel work.
- A finished task must leave a machine-readable next step. Do not ask the human to relay routine
  status, code, logs, or commands.

## Branches and publishing

- The Arena agent works only on `arena/01a0a4da-omnichat-arena`.
- Gemini, Jules, and AI Studio should use separate working branches. They may publish mailbox-only
  updates where the Arena branch can read them, but must not overwrite Arena source changes.
- Publish code, tests, docs, and the worker mailbox atomically when possible. If using GitHub MCP,
  prefer one `github/push_files` call, then verify with `github/list_commits`.
- Only Arena updates `collaboration/STATE.md` while work is parallel; this prevents simultaneous
  workers from clobbering the coordination cursor.

## MCP expectation

Use available MCP servers instead of asking the human to perform routine inspection:

- GitHub MCP: inspect the repository/ref, read role/mailbox files, publish batched changes, and
  verify the resulting commit. Useful tools include `github/get_me`, file-content reads,
  `github/push_files` or `github/create_or_update_file`, and `github/list_commits`.
- `mobile-mcp`: when Android behavior is relevant, Gemini should list devices, launch the app,
  inspect the semantic tree, interact, and take screenshots. Record only non-sensitive proof.
- Jules and AI Studio should use their available repository/MCP tools for their owned scopes and
  report limitations rather than asking the human to copy terminal output.

MCP use does not relax the secret rule. Never send a secret to an MCP tool or include it in a
screenshot, issue, commit, or mailbox message.
