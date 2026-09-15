# 🤖 BRIEF FOR GEMINI AGENT (Antigravity 2.0) — OmniChat Arena

> **You are the build-and-debug agent.** The human (Hilal, non-technical, Windows 11,
> Bursa/Turkey) will do zero coding — you do everything via terminal + adb. I am the
> **Arena architect agent** who designed the app and wrote the scaffold; the human relays
> messages between us. Read this file + the repo, then execute the task list in §5.
> After EACH task, report back in the format in §7 so the human can paste it to me.

---

## 1. The goal (one paragraph)

Build **OmniChat Arena**: a 100% native Android app (Kotlin + Jetpack Compose, NO WebViews
for chat, NO Chromium wrapper) that chats with many AI providers through **the user's OWN
free/subscription logins** (pure sessions — see §2 why), saves all chats **encrypted locally**,
and has an **Arena/compare mode**: one prompt → N selected AIs answer in parallel → a free
LLM judge crowns a 🏆 winner (+ optional fused "best of all" answer). **Hard constraints:**
**(a) the user must NEVER pay for anything — $0-only, no paid APIs/SDKs; (b) chat UI is
native; a login page may open in a Custom Tab ONLY to capture a session, then closes.**

## 2. Where we come from (decision log — do not relitigate)

| Date | Decision |
|------|----------|
| 2026-09-14 | **Pure session logins, no API keys as the strategy.** API keys cost per-token AND can't reach subscription-only models (user has Gemini Pro with a Pro-only model "Flash 3.8" invisible to the free API key). Sessions reuse exactly what the user already has. |
| 2026-09-14 | **$0-only mandate.** Anything paid is banned (DeepSeek API removed from DI for this reason). Free tiers with no card only. |
| 2026-09-14 | **Duck.ai PARKED (bot-walled).** Live spike: old `x-vqd-4` flow dead → JS "Fraud & Abuse" challenge → HTTP 418 `ERR_CHALLENGE` on ALL networks incl. user's home. Even public solver `p2d-duck 1.3.1` 418s today. Proof: `docs/spike-duckai-2026-09-14.md`, repro: `spike_duckai.py`. Retry quarterly, do not burn time now. |
| 2026-09-14 | **Arena DROPPED.** Confirmed arena.ai = lmarena.ai (same site). No chat/prompt API exists (battle/vote flow only) → user said "if it cannot be integrated dont bother". Out of scope. |
| 2026-09-14 | **Interim free engines so the app works on day one:** Pollinations (no key, shared models — flaky, budget-capped) + Groq free tier (user HAS a key, reliable, also the default $0 judge). Gemini free API ($0, no card) stays as interim fallback only. |
| 2026-09-14 | **Provider order:** free engines ✅ → **Gemini session (user's Pro!)** → DeepSeek session → Perplexity session → Claude session → Grok session. |

## 3. What exists (I wrote and live-tested what I could)

**Repo map** (all paths under `omnichat-arena/`):

| Path | What it is |
|------|------------|
| `README.md` | Human-facing quickstart |
| `HANDOFF_TO_GEMINI.md` | THIS file (your orders) |
| `spike_duckai.py` | Duck.ai probe (stdlib-only, still 418s — the retry tool) |
| `docs/spike-duckai-2026-09-14.md` | Duck.ai spike evidence + retry procedure |
| `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` | Gradle 8.x, AGP 8.7.3, Kotlin 2.1.0, KSP, Hilt, Compose BOM |
| `app/build.gradle.kts` | minSdk 26, target/compile 35, R8 on release |
| `app/src/main/AndroidManifest.xml` | INTERNET perm, `omnichat://oauth-callback` deep link (for login capture) |
| `…/OmniApp.kt`, `…/MainActivity.kt` | Hilt app + 3-tab scaffold (Chat / Arena / Keys) |
| `…/core/Providers.kt` | **THE interface**: `AiProvider.streamChat(): Flow<StreamEvent>`, `ProviderId` (9 entries incl. dropped/parked), offline `FakeProvider` |
| `…/core/Judge.kt` | Judge v0 (on-device fast signals) + `JUDGE_PROMPT` for v1 (Groq-as-judge) |
| `…/data/ChatDatabase.kt` | Room entities/DAO (conversations, messages) |
| `…/data/SecretStore.kt` | Keystore-backed encrypted prefs — **ALL keys/sessions live here, never plaintext, never logged** |
| `…/di/AppModule.kt` | Hilt: OkHttp (90s SSE reads), SQLCipher Room, provider multibindings |
| `…/providers/Sse.kt`, `OpenAiCompat.kt` | Shared SSE reader + OpenAI-compatible streaming client |
| `…/providers/GroqProvider.kt` | Groq free tier ✅ (key supplied by user at runtime) |
| `…/providers/PollinationsProvider.kt` | Pollinations, keyless ✅ (flaky — has empty-stream TODO) |
| `…/providers/GeminiProvider.kt` | Gemini API, free interim fallback ($0, no card) |
| `…/providers/DeepSeekProvider.kt` | ⛔ DISABLED + unbound (paid API, violates $0 rule). Session version comes in T5. |
| `…/providers/DuckAiProvider.kt` | ⛔ PARKED (bot-walled). Do not touch until spike passes. |
| `…/ui/ChatScreen.kt` (+VM) | Solo chat, provider picker, streaming |
| `…/ui/CompareScreen.kt` (+VM) | Arena fan-out (`supervisorScope`+`async`) + verdict card |
| `…/ui/SettingsScreen.kt` (+VM) | Key entry per provider |

**Live-verified by me (2026-09-14):** Gemini endpoint reachable (`API_KEY_INVALID` shape ✅);
DeepSeek reachable (`authentication_error` ✅); Pollinations `/models` + `/openai` reachable
(shared key budget-capped/flaky ⚠️); Duck.ai 418 everywhere ⛔ (sandbox AND user's home).
**NOT verified: the Android build itself** (my sandbox has no Android SDK — see T0).

**Full plan:** `../ai-arena-app-plan.md` (architecture, data model, roadmap, risks).

## 4. Your environment (what you have that I don't)

- Windows 11, Antigravity 2.0 with full terminal, project folder with all files above.
- adb + the user's physical phone. Install with:
  `adb devices` → `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- No `gradlew` wrapper in repo yet — first open generates it (Android Studio), or run
  `gradle wrapper --gradle-version 8.9` if a Gradle dist is available.
- The user will paste the Groq key THEMSELVES into the phone app (Keys tab).
  **The key must NEVER be written to any file, log, or chat by you.** If you see a key in
  pasted output, redact it (`gsk_***`) before reporting back.

## 5. Task list (in order — do not skip T0/T1)

- [x] **T0 — BUILD & RUN (first!).** Sync Gradle, `.\gradlew.bat assembleDebug`, install via
  adb, launch, confirm 3 tabs render and Demo provider chats. **Expect small breakages** —
  I couldn't compile. Known risk spots to check FIRST:
  1. Hilt `Map<String, AiProvider>` multibindings + `@JvmSuppressWildcards` in ViewModels.
  2. SQLCipher: verify artifact is `net.zetetic:android-database-sqlcipher:4.5.4` (NOT the old
     `sqlcipher-android` coordinate) and class `net.sqlcipher.database.SupportOpenHelperFactory`
     exists; `sqlite-bundled` may conflict — drop it if Room complains.
  3. `EncryptedSharedPreferences` + `MasterKey.Builder` API (alpha API drifts — adapt).
  4. `AppModule.database()`: my DB-passphrase slot hack (`ProviderId.FAKE + "_DB"` + a silly
     `plus` operator) is UGLY but should compile — replace with a clean dedicated slot.
  5. `OpenAiCompat` references `DuckAiProvider.UA` — fine (const), or move UA to `Sse`.
  Fix, rebuild, re-install until green. Definition of done: Demo chat + Pollinations chat work
  ON THE PHONE (Pollinations needs internet; on failure show the in-app error text).
- [ ] **T1 — PERSISTENCE.** Wire Room: save user/AI messages + conversations in
  `ChatViewModel`/`CompareViewModel` (TODOs in code), load history on start. Done = chats
  survive app restart; airplane-mode library browsing works.
- [ ] **T2 — GROQ E2E + JUDGE v0.** Human pastes Groq key into Keys tab → run Arena with
  Pollinations+Groq+Demo → verdict card appears. Handle Pollinations empty-stream via the
  non-streaming fallback TODO in `OpenAiCompat`. Done = screenshot-able 🏆 verdict on phone.
- [ ] **T3 — GEMINI PRO SESSION (the crown jewel).** Research CURRENT gemini.google.com
  session auth (cookie names like `__Secure-1PSID`, internal streaming endpoints — many
  public write-ups exist; prefer the simplest stable one). Implement: Custom-Tab login →
  `omnichat://oauth-callback` capture → cookies into `SecretStore` → NEW `GeminiSessionProvider`
  (keep `GeminiProvider` API version as fallback; sessions take picker priority) → model
  list incl. Pro-only models. Prove the user's Pro model answers. Session expiry must show
  a clean "re-login" prompt, never a crash. **Spike-first rule: prove the HTTP flow in a
  small script BEFORE writing the provider** (same discipline as `spike_duckai.py`).
- [ ] **T4 — JUDGE v1.** Groq-as-judge: shuffle + anonymize (A/B/C…) answers, score with
  `JUDGE_PROMPT`, show rationale + optional fused "best of all" answer toggle.
- [ ] **T5+ — MORE SESSIONS, one at a time:** DeepSeek → Perplexity → Claude → Grok.
  Each = spike script first, then provider + `@Binds` line + Settings row + health check.
  Warn the human in-app + in your report about ToS/account-ban risk on Claude/Grok sessions.
- [ ] **T6 — HARDENING.** Per-provider kill-switches, export (MD/JSON), search, biometric
  lock option, R8/release build, dark/light + accessibility pass.

## 6. Rules (violating these = failed task)

1. **$0-only.** No paid API, SDK, or service. Free tiers must need no card (Groq/GS-free OK).
2. **No WebViews for chat.** Only an invisible session-capture Custom Tab for logins (T3+).
3. **Secrets:** Keystore/`SecretStore` only. Never log, print, or commit secrets. Redact in reports.
4. **Spike-first for every unofficial integration** (script proves it → then provider code).
5. **Small steps, always building:** never leave the repo in a non-compiling state at task end.
6. **Dropped/parked stay dropped/parked:** Arena (out), Duck.ai (retry only via `spike_duckai.py`
   passing first), DeepSeek API (paid — session only in T5).
7. **Ask the human ONLY for:** pasting keys into the phone app, tapping through login pages,
   on-phone testing. Everything else you do yourself.

## 7. Report format (paste back after EVERY task)

```
TASK: T# + name — DONE / BLOCKED
BUILD: ✅ apk installs + launches | ❌ error: <first error line>
FILES CHANGED: <list>
PROOF: <what you ran on phone/emulator + result, or logcat snippet if blocked>
SECRETS: none touched / handled per §6.3
NEXT: <one line>
```

## 8. Two-agent workflow

- The human relays between us. If you're unsure about ARCHITECTURE (interfaces, where code
  lives, provider design), write the question in your report and the human will ask the Arena
  agent (me) — I'll answer with exact code/edits. If it's BUILD/DEBUG (Gradle, adb, logcat,
  UI polish), that's YOUR call — just decide and do it.
- If you discover my scaffold has a real bug, fix it and note it in FILES CHANGED — don't wait.

— Arena architect agent, 2026-09-14. Good luck. Ship it. 🚀
