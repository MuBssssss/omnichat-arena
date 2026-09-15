# Agent messaging protocol

## Purpose

This is a zero-relay, repository-backed mailbox for the Arena architect agent and the Gemini
build/debug agent. It removes the need for the human to copy code, logs, or routine progress
between agents. GitHub is the transport; these files are the stable addresses:

| Address | Owner when writing | Meaning |
|---|---|---|
| `collaboration/messages/arena-to-gemini.md` | Arena | Latest task or review for Gemini |
| `collaboration/messages/gemini-to-arena.md` | Gemini | Latest implementation report or question |
| `collaboration/STATE.md` | The agent that just acted | Current cursor, status, and next owner |
| `collaboration/next_commands_for_gemini.md` | Arena | Executable next-step instructions |

The mailbox files contain the latest state. Earlier versions remain recoverable in Git history;
do not create a growing collection of copied logs or commit generated dumps.

## Standard loop

### Before changing code

1. Read `AGENTS.md`, this protocol, `collaboration/STATE.md`, and your addressed mailbox.
2. Use GitHub MCP to inspect the repository, the relevant branch/ref, and recent commits. Do not
   assume that a local checkout is current.
3. Check the task against `HANDOFF_TO_GEMINI.md` and `ai-arena-app-plan.md` before expanding
   scope.
4. If Android behavior is involved, use `mobile-mcp` for device discovery and inspection rather
   than asking the human to collect screenshots or UI details.

### After changing code

1. Run the smallest useful validation, then the build or device test when available.
2. Update the addressed mailbox with the exact files changed, proof, blockers, and one next step.
3. Update `collaboration/STATE.md` with the same message id and status.
4. Publish source changes, mailbox changes, and state together when possible. With GitHub MCP,
   prefer one `github/push_files` operation; with Git, commit and push only the agent's allowed
   branch.
5. Verify the published commit with `github/list_commits` or `git log`/`git ls-remote`. The other
   agent should be able to continue without a human relay.

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
"done" or points to a local Windows path that the other agent cannot access.

## Branch/mailbox rule

The Arena agent is fixed to `arena/01a0a4da-omnichat-arena`. Gemini can work on a separate code
branch, but its response must be published where that branch can be read. If the GitHub MCP tool
supports a `branch`/`ref` argument, target the Arena branch for mailbox-only updates. Do not use
that mechanism to overwrite Arena source files. If the tool does not support a ref, publish the
response on Gemini's branch and put the exact branch name and commit in `collaboration/STATE.md`.

## GitHub MCP checklist for Gemini

Use the MCP server fully for repository collaboration:

1. `github/get_me` — confirm the authenticated identity if the repository or permissions are
   unclear.
2. Repository/file reads — inspect the current ref, mailbox, plan, and changed files before
   editing.
3. `github/push_files` — publish a coherent batch of implementation, tests/docs, mailbox, and
   state files rather than making the human relay individual snippets.
4. `github/create_or_update_file` — use for a small mailbox-only acknowledgement when a batch is
   unnecessary.
5. `github/list_commits` — verify the commit landed and report its short SHA.

Use `mobile-mcp` whenever the task has an Android/device acceptance criterion: list devices,
launch the package, inspect elements, interact, and capture a screenshot. Never include a key,
cookie, session identifier, or sensitive user data in tool arguments or proof.

## Minimal-interaction completion format

The agent's user-facing completion should be a short handoff, not a request for the human to
retype implementation details:

```text
done, i've done all changes needed on my end
copy-paste this message to gemini telling it:
"look at [files]. i've made changes [list]. your next commands are in [file]."
```

The same sentence is written to the mailbox so Gemini receives it without a relay. For a blocked
task, replace `done` with `blocked` and include only the missing decision or human action.
