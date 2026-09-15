# Implementation Plan: T4.2 Polish, DeepSeek Parking, GitHub Collab & T6 Perplexity Spike

Comprehensive plan and engineering record for T4.2 bug fix (Judge regex), DeepSeek parking, role-consumer audit, GitHub repo setup, and upcoming T6 Perplexity web session spike.

## Status: T4.2 100% COMPLETE & VERIFIED ON HARDWARE (ALL GREEN ✅) | GITHUB REPO LIVE | MOBILE MCP ACTIVE

---

## 1. Security Remediation & Secret Hygiene (Standing Rules)
- **Zero Raw Secrets:** Redact all `gsk_*`, `PSID`, `userToken`, and session identifiers in all reports, terminal pastes, and code repositories.
- **Rotation Reminders:** User reminder maintained for Google Session Cookies ([myaccount.google.com/device-activity](https://myaccount.google.com/device-activity)) and Groq free API key ([console.groq.com/keys](https://console.groq.com/keys)).
- **S2 In-App Security:** `SET_SECRET` intent handler permanently eliminated from `MainActivity.kt`.

---

## 2. T4.2 Bug Fixes & Architectural Alignment (Architect Orders)

### T4.2-P0: Judge Letter-"A" De-Anonymization Regex Fix (`Judge.kt`)
- **Problem:** Broad regex replacing standalone letter "A" / "a" with model name mangled ordinary English words (e.g. `"better than a simple list"` became `"better than Gemini simple list"`).
- **Fix:** Deleted broad case-insensitive letter-"A" fallback regexes; retained exact contender tag replacements (`\bContender A\b`, `\bModel A\b`, `\bA's\b`) and bare-letter replacements only for non-A letters (`B` through `Z`).
- **Verified:** Class names `JudgeEngine` and `JudgeScore` preserved; score descending sorting preserved.

### DeepSeek Parked per $0 Mandate (`Providers.kt`, `SettingsScreen.kt`)
- **Action:**
  - In `Providers.kt`: set `selectable = false` for `ProviderId.DEEPSEEK` (`// bot-walled, parked (T5a)`).
  - In `SettingsScreen.kt`: updated DeepSeek card text to `"DeepSeek — ⛔ bot-walled, parked"` and subtitle `"AWS WAF JS-challenge + 144k PoW puzzle (Sep 2026 spike). Parked per $0 plan."`

### Role-Consumer Audit (`ChatDatabase.kt`, `ChatScreen.kt`, `CompareScreen.kt`)
- **Audit Result:** Verified `MessageEntity.role` usage (`"USER"`, `"AI"`, `"VERDICT"`). Consistent across Room DB schema, `ChatDao`, and UI composables.

---

## 3. Tooling & Collaboration Infrastructure

### GitHub Repository Setup
- **Repository URL:** `https://github.com/MuBssssss/omnichat-arena`
- **Owner:** `MuBssssss`, **Branch:** `main`
- **MCP Integration:** GitHub MCP server enabled for automated push, commit, and code synchronization between Antigravity and `arena.ai`.

### Mobile MCP Automation
- **Server:** `mobile-mcp`
- **Device:** `192.168.1.102:5555` (`SM-J701F`, Samsung Galaxy J7 Nxt, LineageOS 17.1)
- **Status:** Online and integrated for direct screenshot capture, view inspection, and hardware testing.

---

## 4. Hardware Verification Table (Samsung Galaxy J7 Nxt)

| Test Item | Verification Details | Proof Artifact |
|---|---|---|
| **T4.2 DeepSeek Card** | Verified in Keys tab that DeepSeek displays parked status and disabled input. | `docs/screenshots/t4_2_keys.png` |
| **T4.2 Contender Chips** | Verified in Arena tab that DeepSeek chip is hidden from contenders. | `docs/screenshots/t4_2_arena_tab.png` |
| **T4.2 De-anonymized Battle** | Live battle Gemini vs Groq: prompt "Why is the sky blue" ran clean with no mangled words. | `docs/screenshots/t4_2_battle_done.png` |
