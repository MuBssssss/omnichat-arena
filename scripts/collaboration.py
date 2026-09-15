#!/usr/bin/env python3
"""Small, dependency-free checks for the repository-backed multi-agent mailbox.

Usage:
    python3 scripts/collaboration.py validate
    python3 scripts/collaboration.py status

The script intentionally never prints file contents. That makes it safe to run in a terminal
where credentials might exist in unrelated local files.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_FILES = (
    "AGENTS.md",
    "collaboration/AGENT_ROLES.md",
    "collaboration/PROTOCOL.md",
    "collaboration/ORDER_OF_OPERATIONS.md",
    "collaboration/STATE.md",
    "collaboration/next_commands_for_gemini.md",
    "collaboration/prompts/JULES.md",
    "collaboration/prompts/AI_STUDIO.md",
    "collaboration/prompts/GEMINI_ANTIGRAVITY.md",
    "collaboration/messages/arena-to-gemini.md",
    "collaboration/messages/gemini-to-arena.md",
    "collaboration/messages/arena-to-jules.md",
    "collaboration/messages/jules-to-arena.md",
    "collaboration/messages/arena-to-aistudio.md",
    "collaboration/messages/aistudio-to-arena.md",
)
MESSAGE_FILES = (
    "collaboration/messages/arena-to-gemini.md",
    "collaboration/messages/gemini-to-arena.md",
    "collaboration/messages/arena-to-jules.md",
    "collaboration/messages/jules-to-arena.md",
    "collaboration/messages/arena-to-aistudio.md",
    "collaboration/messages/aistudio-to-arena.md",
)
REQUIRED_MESSAGE_HEADINGS = (
    "## Files changed",
    "## Proof",
    "## Next commands",
    "## Reply required",
)
# These patterns target values, not safe documentation such as `gsk_***` or a cookie name.
SECRET_PATTERNS = (
    re.compile(r"\bgsk_[A-Za-z0-9]{12,}\b"),
    re.compile(r"\bAIza[0-9A-Za-z_-]{20,}\b"),
    re.compile(r"\bsk-[A-Za-z0-9]{20,}\b"),
    re.compile(r"(?i)\bbearer\s+[A-Za-z0-9._-]{24,}"),
    re.compile(r"(?i)(?:PSID|PSIDTS|userToken)\s*[:=]\s*['\"]?[^*\s'\"`]{12,}"),
)


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def validate() -> int:
    errors: list[str] = []
    for relative in REQUIRED_FILES:
        path = ROOT / relative
        if not path.is_file():
            errors.append(f"missing {relative}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1

    protocol = read("collaboration/PROTOCOL.md")
    for heading in REQUIRED_MESSAGE_HEADINGS:
        if heading not in protocol:
            errors.append(f"protocol is missing {heading}")

    for relative in MESSAGE_FILES:
        content = read(relative)
        for heading in REQUIRED_MESSAGE_HEADINGS:
            if heading not in content:
                errors.append(f"{relative} is missing {heading}")
        if "Message ID:" not in content or "Status:" not in content:
            errors.append(f"{relative} is missing message metadata")
        for pattern in SECRET_PATTERNS:
            if pattern.search(content):
                errors.append(f"possible secret value in {relative} (pattern {pattern.pattern})")

    state = read("collaboration/STATE.md")
    for marker in ("Last message ID:", "Status:", "Next owner:", "Next commands:", "Reply mailbox:"):
        if marker not in state:
            errors.append(f"STATE.md is missing {marker}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1

    print("collaboration validation: OK")
    print(f"required files: {len(REQUIRED_FILES)}")
    print(f"mailboxes checked: {len(MESSAGE_FILES)}")
    print("secret-value scan: OK")
    return 0


def status() -> int:
    state = read("collaboration/STATE.md")
    wanted = (
        "Last message ID:",
        "Last sender:",
        "Last recipient:",
        "Status:",
        "Published ref:",
        "Last commit:",
        "Next owner:",
        "Next commands:",
        "Reply mailbox:",
    )
    print("collaboration status")
    for line in state.splitlines():
        if any(line.startswith(f"- {marker}") for marker in wanted):
            print(line)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate or inspect the agent mailbox")
    parser.add_argument("command", choices=("validate", "status"))
    args = parser.parse_args()
    return validate() if args.command == "validate" else status()


if __name__ == "__main__":
    sys.exit(main())
