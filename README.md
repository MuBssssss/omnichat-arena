# OmniChat Arena

Native Android app: Multi-AI providers behind one `AiProvider` interface, encrypted Room/SQLCipher chat persistence, and an arena/compare mode with on-device LLM judging.

## Agent collaboration

This repository is also the zero-relay handoff network for the Arena architect and Gemini build/debug agents. Before editing, read [`AGENTS.md`](AGENTS.md), [`collaboration/PROTOCOL.md`](collaboration/PROTOCOL.md), and [`collaboration/STATE.md`](collaboration/STATE.md). The current task and reply mailboxes are in [`collaboration/messages/`](collaboration/messages/); executable Gemini instructions are in [`collaboration/next_commands_for_gemini.md`](collaboration/next_commands_for_gemini.md).

Validate the mailbox without dependencies:

```bash
python3 scripts/collaboration.py validate
```

## Status: T4.1 / T4.2 Verified on Hardware (Samsung Galaxy J7 Nxt - SM-J701F)
- $0-Only Architecture: Groq free tier API key + Gemini Pro web session integration.
- Hardware-backed Keystore AES-256-GCM encryption for keys and session cookies.
- Blind anonymized LLM Judge with synthesized fused answers and on-device fallback.
