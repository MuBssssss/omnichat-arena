# Multi-agent messaging protocol

## Purpose

This is a zero-relay, repository-backed mailbox for Arena, Gemini/Antigravity, Jules, and AI
Studio. GitHub is the transport; the stable role/ownership rules are in
`collaboration/AGENT_ROLES.md`, and the launch sequence is in
`collaboration/ORDER_OF_OPERATIONS.md`.

| Address | Owner when writing | Meaning |
|---|---|---|
| `collaboration/messages/arena-to-gemini.md` | Arena | Main integrator task/review |
| `collaboration/messages/gemini-to-arena.md` | Gemini | Integration/build/device report |
| `collaboration/messages/arena-to-jules.md` | Arena | Jules spike task/review |
| `collaboration/messages/jules-to-arena.md` | Jules | Jules research/spike report |
| `collaboration/messages/arena-to-aistudio.md` | Arena | AI Studio UI task/review |
| `collaboration/messages/aistudio-to-arena.md` | AI Studio | AI Studio UI/audit report |
| `collaboration/STATE.md` | Arena only during parallel work | Canonical coordination cursor |
| `collaboration/prompts/` | Arena | Copy-paste role briefs |

Mailbox files contain the latest state. Earlier versions remain recoverable in Git history; do
not create copied logs or commit generated dumps.

## Standard loop

### Before changing code

1. Read `AGENTS.md`, `collaboration/AGENT_ROLES.md`, this protocol,
   `collaboration/ORDER_OF_OPERATIONS.md`, `collaboration/STATE.md`, your prompt, and your
   addressed mailbox.
2. Use GitHub MCP to inspect the repository, relevant ref, and recent commits. Do not assume a
   local checkout is current.
3. Check the task against `HANDOFF_TO_GEMINI.md` and `ai-arena-app-plan.md` before expanding
   scope.
4. Respect the ownership table. If another agent owns a file, report a conflict to Arena instead
   of silently overwriting it.
5. If Android behavior is involved, Gemini uses `mobile-mcp` for device discovery and inspection.

### After changing code

1. Run the smallest useful validation, then the build or device test when available.
2. Update your own addressed mailbox with exact files, proof, blockers, and one next step.
3. Publish source changes and your mailbox together when possible. With GitHub MCP, prefer one
   `github/push_files` operation; with Git, push only your allowed branch.
4. Include the exact published branch/ref and commit SHA in your mailbox.
5. Do **not** update `collaboration/STATE.md` as a worker during parallel work. Arena updates the
   canonical cursor after reviewing each worker mailbox, preventing concurrent overwrites.
6. Verify the published commit with `github/list_commits` or `git log`/`git ls-remote`.

## Message contract

Every mailbox message must contain these headings:

```text
# Message: <sender> -> <recipient>
- Message ID: <stable id>
- Status: ACTION_REQUIRED | IN_PROGRESS | DONE | BLOCKED | ACKNOWLEDGED
- Updated: <UTC ISO-8601 timestamp>
- Branch/ref: <where the message is published>

## Files changed
- `<repo-relative path>` — <what changed>

## Proof
- <commands, build result, MCP/device result, or the first blocking error>

## Next commands
- Read `<repo-relative path>` and run the next task from it.

## Reply required
- <exact mailbox to update, or `none`>
```

Use repository-relative paths and concise summaries. A message is not complete if it says only
"done" or points to an inaccessible local path.

## Branch/mailbox rule

The Arena agent is fixed to `arena/01a0a4da-omnichat-arena`. Gemini, Jules, and AI Studio should
work on separate branches or isolated file sets. If a GitHub MCP tool supports a `branch`/`ref`
argument, it may target the Arena branch for a mailbox-only update; never use that to overwrite
another agent's source files. Workers record their own branch/ref/SHA in their outbound mailbox.

## GitHub MCP checklist

Every agent should use the available GitHub MCP fully:

1. `github/get_me` when identity or permissions are unclear.
2. Repository/file reads for roles, mailbox, plan, current ref, and changed files.
3. `github/push_files` for a coherent batch of owned code/docs and the outbound mailbox.
4. `github/create_or_update_file` for a small mailbox acknowledgement when a batch is unnecessary.
5. `github/list_commits` and `github/get_commit` to verify the published SHA.

Use `mobile-mcp` whenever Android behavior has an acceptance criterion: list devices, launch the
package, inspect elements, interact, and capture non-sensitive proof. Never include a key, cookie,
session identifier, account data, or private response in tool arguments or proof.

## Minimal-interaction completion format

The agent's user-facing completion should be a short handoff, not a request for the human to
retype implementation details:

```text
done, i've done all changes needed on my end
copy-paste this message to <next agent> telling it:
"look at [files]. i've made changes [list]. your next commands are in [file]."
```

For a blocked task, replace `done` with `blocked` and include only the missing decision or human
action. The mailbox is the relay.
