# OmniChat Arena

Native Android app: Multi-AI providers behind one `AiProvider` interface, encrypted Room/SQLCipher chat persistence, and an arena/compare mode with on-device LLM judging.

## Agent collaboration

This repository is also the zero-relay handoff network for four agents: Arena (architecture/review), Gemini/Antigravity (main coder/build/device tester), Jules (provider spikes), and AI Studio (Android UI/UX/accessibility). Before editing, read [`AGENTS.md`](AGENTS.md), [`collaboration/AGENT_ROLES.md`](collaboration/AGENT_ROLES.md), [`collaboration/PROTOCOL.md`](collaboration/PROTOCOL.md), and [`collaboration/ORDER_OF_OPERATIONS.md`](collaboration/ORDER_OF_OPERATIONS.md). The human launch sequence and ready-to-copy messages are in [`collaboration/HUMAN_LAUNCH_GUIDE.md`](collaboration/HUMAN_LAUNCH_GUIDE.md). The current task and reply mailboxes are in [`collaboration/messages/`](collaboration/messages/); copy-paste role briefs are in [`collaboration/prompts/`](collaboration/prompts/).

Validate the mailbox without dependencies:

```bash
python3 scripts/collaboration.py validate
```

## Status: T4.1 / T4.2 Verified on Hardware (Samsung Galaxy J7 Nxt - SM-J701F)
- $0-Only Architecture: Groq free tier API key + Gemini Pro web session integration.
- Hardware-backed Keystore AES-256-GCM encryption for keys and session cookies.
- Blind anonymized LLM Judge with synthesized fused answers and on-device fallback.
