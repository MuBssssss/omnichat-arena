# Project "OmniChat Arena" — Android Build Plan
**Concept:** one native Android app that chats with Perplexity, Gemini, Grok, Claude and DeepSeek through the user's *own logins* (no paid API keys) — plus free engines (Groq free tier, Pollinations) — with local chat history and a "compare mode" that picks/shows the best answer. (Duck.ai parked — bot-walled; Arena dropped — no API.)

**Date:** 2026-09-14 · **Status:** v3 — ✅ pure sessions chosen · $0-only mandate · Arena dropped · DeepSeek API removed (paid) · Gemini session next · Gemini-agent handoff written (`omnichat-arena/HANDOFF_TO_GEMINI.md`)

---

## 1. TL;DR — the honest feasibility summary

| # | Provider | Official API? | Free path? | "Use my login, no API key, no WebView" path | Difficulty |
|---|----------|---------------|------------|----------------------------------------------|------------|
| 1 | Gemini | ✅ Yes | ✅ Free API tier (interim fallback) | ✅ **Primary: Pro session** (user's Gemini Pro → Flash 3.8, invisible to API keys) | ⭐⭐ Medium |
| 2 | DeepSeek | ✅ Yes | ⛔ API is paid → BANNED by $0 mandate; free path = chat.deepseek.com session (free account) | ✅ Free-account session replay | ⭐⭐ Medium |
| 3 | Duck.ai | ❌ No | ⛔ SPIKE FAILED 2026-09-14: JS challenge + 418 on ALL networks incl. user's home — PARKED | ⛔ Blocked until public solvers recover; retry quarterly | 🔴 Blocked |
| 4 | Perplexity | ✅ Yes (Sonar) | ⚠️ Sonar API paid → banned; session uses free/Pro web account | ⚠️ Replay web API with user's session cookie (fragile, ToS grey zone) | ⭐⭐⭐ Hard |
| 5 | Claude | ✅ Yes | ⚠️ API pay-per-token → banned; session uses free/Pro web account | ⚠️ Reverse-engineer claude.ai internal API with session key — works, breaks often, ban risk | ⭐⭐⭐ Hard + risky |
| 6 | Grok | ✅ Yes | ⚠️ API paid → banned; session uses free/X-Premium web account | ⚠️ X/xAI OAuth + internal API replay — complex auth, ban risk | ⭐⭐⭐ Hard + risky |
| 7 | Arena (= arena.ai = LMArena, confirmed same site) | ❌ No chat API | ✅ Free on web | 🔴 No prompt endpoint; battle/vote flow only → DROPPED per user ("if it cannot be integrated dont bother") | ➖ Dropped |
| — | Groq (free tier) ✅ | ✅ Yes (OpenAI-compat) | ✅ Free key, verified live 2026-09-14 (models: `openai/gpt-oss-120b`, `qwen/qwen3.8-27b`) | n/a — keyless-to-user free tier, also the default $0 judge | ⭐ Easy |
| — | Pollinations ✅ | ✅ OpenAI-compat | ✅ No key at all (shared, budget-capped/flaky) | n/a — free extra engine | ⭐ Easy |

**Bottom line:** your exact spec ("native, my own logins, no API keys, no browser") is *technically buildable* but means maintaining unofficial session integrations that can break without notice. Per your decision, that's the plan:

> **User decision (2026-09-14): pure session logins.** API keys rejected — they cost per-token AND can't reach subscription-only models (e.g. user's Gemini Pro → Flash 3.8 is invisible to the free API key). Free engines (Pollinations/Groq) cover day-one chatting + $0 judging while sessions are built.

Details in §3. The `AiProvider` interface keeps every engine swappable.

---

## 2. Product definition

### 2.1 Core loops
1. **Solo chat** — pick one AI → chat → history saved locally.
2. **Arena/compare mode** — pick 2–6 AIs → one prompt fans out in parallel → answers stream side-by-side → a **Judge** crowns a "Best Answer" (and optionally fuses one merged answer).
3. **Library** — all chats searchable locally, exportable (Markdown/JSON), no cloud account needed.

### 2.2 Non-goals for v1 (to stay shippable)
- No iOS/web client, no cloud sync, no team features.
- No image/video generation (text chat first; attach images later).
- No prompt marketplace, no plugins.
- Nothing that costs the user money ($0 mandate — see §8).

### 2.3 Success criteria for MVP
- [x] Scaffold + free engines working natively (Pollinations, Groq, Fake) — done 2026-09-14.
- [ ] Gemini Pro session working (the crown jewel: user's Flash 3.8 in-app).
- [ ] Compare mode with 2+ providers, parallel streaming, judge verdict.
- [ ] Login/session persists across restarts; tokens in Keystore-encrypted storage.
- [ ] All chats in encrypted local DB; export works; airplane-mode library browsing works.
- [ ] No WebView rendering chat sites (login capture via Custom Tab only — approved).

---

## 3. The big decision: auth architecture

### Option A — BYOS: "Bring Your Own Session" ✅ CHOSEN
User logs into each service; app captures the session (cookies/tokens) and calls the service's **private web APIs directly with OkHttp** — fully native UI, zero WebViews for chat.
- **Login capture without WebView:** open the real login page in a **Custom Tab** (outside WebView, so Google/X SSO + CAPTCHA + 2FA all work), intercept the redirect/cookies via your `app://oauth-callback` deep link + CookieManager sync, then close the tab. 100% of *chatting* stays native.
- Pros: matches your vision; user "uses their subscription" (e.g. Gemini Pro → Flash 3.8) instead of paying per token.
- Cons: violates most providers' ToS (automation/scraping clauses); endpoints change without notice; account-ban risk on Claude/Grok; high maintenance (~1 breakage/quarter per provider is normal for this genre).

### Option B — BYOK: "Bring Your Own Key" ❌ REJECTED (except $0 free tiers)
User rejected: costs per-token AND can't reach subscription-only models. Only $0 exceptions survive: Groq free tier (also the judge), Pollinations (keyless), Gemini free API (interim fallback). DeepSeek API removed from the app for violating the $0 rule.

### ✅ DECISION 2026-09-14: pure sessions (Option A) — user's choice
Why: sessions use exactly what the user already pays for (or gets free). Interim free engines ($0, no subscriptions): **Pollinations** (no key at all, shared models — budget-capped/flaky, verified Sep 2026) + **Groq free tier** (own free key, reliable, also the default $0 JUDGE). Compare mode works on day one while sessions land.

---

## 4. System architecture

```
┌─────────────────────────────────────────────────────────────┐
│  UI (Jetpack Compose + Material 3, single-activity)          │
│  ChatScreen · CompareScreen · LibraryScreen · Settings      │
└──────────────┬──────────────────────────────────────────────┘
               │ StateFlow / Nav Compose
┌──────────────▼──────────────────────────────────────────────┐
│  Domain: UseCases (SendMessage, RunArena, JudgeAnswers,     │
│  ExportChat) · Entities (Conversation, Message, Verdict)     │
└──────────────┬──────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────┐
│  Data: AiProvider interface ← N implementations              │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌──────┐        │
│  │ Gemini │ │ Groq   │ │Pollinat│ │Perplex.│ │Claude│ …      │
│  │(sess.) │ │(free)  │ │(free)  │ │(sess.) │ │(sess)│        │
│  └────────┘ └────────┘ └────────┘ └────────┘ └──────┘        │
│  SessionManager (Keystore) · JudgeEngine · ChatRepository   │
└──────────────┬──────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────┐
│  Local: Room + SQLCipher · DataStore · Files (exports)      │
└─────────────────────────────────────────────────────────────┘
Network: OkHttp + Retrofit, SSE streaming reader, per-provider
header/cookie jars, retry + backoff, request fingerprinting.
```

### 4.1 Key design rules
- **One interface for everything:** `suspend fun streamChat(req): Flow<TokenDelta>` + `authType()` + `models()` + `healthCheck()`. UI never knows which provider is API vs session.
- **Provider isolation:** each provider = own module/package with own DTOs, so when Claude changes its API, only `provider-claude` breaks.
- **Kill-switches:** Remote-config-style local flags (or Firebase Remote Config later) to disable a broken provider without an app update.
- **No backend server in v1** — everything on-device. (Optional later: tiny relay only if a provider needs server-side secret handling.)
- **Spike-first for every unofficial integration:** a script proves the HTTP flow before provider code is written.

---

## 5. Tech stack (recommended)

| Layer | Choice | Why |
|-------|--------|-----|
| Language | Kotlin 2.x, Coroutines + Flow | Standard, structured concurrency for parallel fan-out |
| UI | Jetpack Compose + Material 3, Navigation Compose | Native, fast iteration, adaptive layouts (phone/foldable) |
| DI | Hilt | Scoped providers, testability |
| Network | OkHttp 5 + Retrofit 2 + kotlinx.serialization, custom SSE reader | Cookies, interceptors, streaming; JSON-safe |
| Local DB | Room 2.7 + SQLCipher (SQLCipher for Android) | Encrypted chat saves |
| Prefs | DataStore (Proto) + EncryptedSharedPreferences | Settings + secrets |
| Secrets | Android Keystore (AES/GCM, biometric-gated optional) | Session tokens never in plaintext |
| Markdown | Compose Markdown renderer (e.g., multiplatform-markdown-renderer) | AI answers are Markdown-heavy |
| Login capture | Custom Tabs + AppAuth (OAuth where supported) | Real SSO without WebView |
| Background | WorkManager | Retries, exports, cleanup |
| Min/target SDK | minSdk 26, targetSdk 35/36 (per 2026 Play rules) | Covers ~95%+ devices |
| Build | Gradle KTS, version catalog, baseline profiles | Reproducible builds |

---

## 6. Data model (v1 sketch)

```kotlin
Conversation(id, title, providerIds[], mode: SOLO|ARENA,
  createdAt, updatedAt, pinned, folder)
Message(id, conversationId, role: USER|AI|VERDICT, providerId?,
  modelName?, text, citations[], tokenCount?, latencyMs?,
  status: STREAMING|DONE|ERROR, createdAt)
ProviderAccount(providerId, authType: API_KEY|SESSION|NONE,
  secretRef /* Keystore alias */, expiresAt, lastValidated)
ArenaRun(id, conversationId, promptHash, answers: Map<providerId, messageId>,
  verdict: JudgeVerdict?)
JudgeVerdict(winnerProviderId, scores: Map<providerId, Float>,
  rationale, fusedAnswer?, judgeModel)
```

---

## 7. Compare/Judge engine ("show the user the best answer")

### 7.1 Fan-out
- Fire one prompt to N selected providers concurrently (`async` per provider, `supervisorScope` so one failure doesn't kill the run).
- Stream each answer live into its own column/card; per-provider cancel + retry.
- Timeout budget: e.g., 60s default, first-token timeout 15s; slow providers marked "lagging" not failed.

### 7.2 Judging (3 layers, all on-device-orchestrated)
1. **Fast signals (free, instant):** latency, length sanity, citation count (Perplexity free tier), error penalty, refusal detection ("I can't…").
2. **LLM-as-judge (the real verdict):** send the anonymized answers (labeled A/B/C, order-shuffled to kill position bias) to a cheap+smart judge — default **Groq free tier ($0)** or Pollinations (fallback). Rubric: correctness, completeness, citations, clarity. Returns scores + rationale + winner.
3. **Fused "Best Answer" (optional toggle):** judge model writes one merged answer citing contributors ("per B…"). This is your killer feature — nobody else fuses this many AIs into one.

### 7.3 UX
- Side-by-side (landscape/foldable) or swipeable cards (portrait); winner gets a 🏆 badge; "Why?" expands the rationale; per-answer 👍/👎 feeds a local ELO board ("your personal arena leaderboard").

---

## 8. Cost & account reality (per-user, personal use)

> **$0 MANDATE (2026-09-14): nothing in this app may cost the user money. Paid APIs (DeepSeek, Claude/Grok/Perplexity tokens) are banned; sessions + free tiers only.**

| Provider | Path in this app | User cost |
|----------|------------------|-----------|
| Groq | Free-tier key (verified live) | **$0** |
| Pollinations | Keyless shared models | **$0** (flaky caps) |
| Gemini | Your Pro session (primary) + free API (interim) | **$0** extra |
| DeepSeek | Free-account session (paid API removed from app) | **$0** |
| Perplexity | Free/Pro web-account session | **$0** extra |
| Claude | Free/Pro web-account session | **$0** extra |
| Grok | Free/X-Premium web-account session | **$0** extra |
| Duck.ai | PARKED (bot-walled) | — |
| Arena | DROPPED (no API) | — |

Your costs as dev: **$25 one-time** Play Console fee (only if/when publishing to Play; sideload via adb is free) + $0 infra (local-only app).

---

## 9. Phased roadmap

### Phase 0 — Validation spikes
- [x] Duck.ai spike → **BLOCKED** (JS challenge + 418 everywhere). Parked. Evidence: `omnichat-arena/docs/spike-duckai-2026-09-14.md`.
- [x] Gemini endpoint reachable (auth-error shape confirmed, no key needed for the check).
- [x] Groq free tier verified live (key supplied by user; models refreshed: `openai/gpt-oss-120b` etc.).
- [x] Pollinations verified (works, shared-key budget caps observed).
- [ ] Custom-Tab login capture loop → proven during Gemini session build (T3).

### Phase 1 — App foundation ✅ SCAFFOLD DONE 2026-09-14
- [x] Gradle + Compose + Hilt + Room/SQLCipher + Keystore secrets + `AiProvider` + Fake.
- [x] Chat screen, Arena screen, Keys screen; Groq + Pollinations + Gemini-fallback providers.
- [ ] Gemini-agent T0: first compile + install + on-phone green (I have no Android SDK — expected small breakages, all flagged in `HANDOFF_TO_GEMINI.md`).
- [ ] T1: Room persistence wiring (save/load chats).

### Phase 2 — Real providers (sessions, spike-first each)
Order: **free engines ✅ → Gemini session (user has Pro!) → DeepSeek session → Perplexity session → Claude session → Grok session**. Duck.ai PARKED. Arena DROPPED.
Each provider ticket: spike script → DTOs → streaming client → session auth → error mapping → health check.
- ✅ Exit per provider: solo chat works, survives restart, handles expiry with a clear "re-login" prompt.

### Phase 3 — Arena mode + Judge
- [x] Multi-select provider picker, parallel `RunArena`, columns/cards UI, fast-signals verdict.
- [ ] Judge v1: Groq-as-judge (shuffled/anonymized) + rationale card + fused-answer toggle.
- ✅ Exit: 3-provider arena run with verdict in <60s on mid-range device.

### Phase 4 — Hardening
- [ ] Token-expiry auto-refresh + re-login deep links; per-provider kill-switches.
- [ ] Export (MD/JSON), search, folders, biometric lock, backup file (encrypted).
- [ ] Baseline profiles, R8, accessibility pass, dark/light, adaptive layouts.
- [ ] Sideload-first distribution (adb); Play listing later (privacy policy, data-safety form).
- ✅ Exit: user testing on their own phone.

### Phase 5 — Post-launch iterations
- Image attachments → vision models; voice input; per-model picker inside providers; prompt templates; widget/shortcuts; optional encrypted cloud sync; Duck.ai retry.

**Estimate with Gemini-agent building:** MVP (sessions for Gemini + 2 free engines + judge) ≈ days–2 weeks; full session lineup ≈ several weeks (each unofficial integration is its own adventure).

---

## 10. Decisions (all answered ✅)

1. **Auth route:** ✅ ANSWERED — pure session logins (API keys rejected: cost + no Pro-model access).
2. **Arena.ai meaning:** ✅ ANSWERED — arena.ai = lmarena.ai (same site, confirmed). DROPPED from scope (no integrable API).
3. **First providers for MVP?** ✅ ANSWERED — free engines (Pollinations + Groq, done) → Gemini session next.
4. **Your setup:** ✅ ANSWERED — Hilal (non-technical) + Gemini agent in Antigravity 2.0 (builds, debugs, adb-pushes to phone) + Arena agent (me: architecture, provider designs, reviews).
5. **Distribution:** ✅ ANSWERED — sideload via adb first (free, no review); Play Store later if ever (session features are ToS-grey for Play review).

---

## 11. Risks & how we mitigate them

| Risk | Mitigation |
|------|------------|
| Provider changes private API | Isolated modules + kill-switch + health checks; spike-first discipline |
| Account bans (Claude/Grok sessions) | Warn users in-app; rate-limit; never share sessions; space out requests |
| Play Store policy (automation/ToS) | Sideload-first; Play only later, possibly with session features in a separate flavor |
| Token theft on-device | Keystore + SQLCipher + no logs of secrets; biometric lock option |
| Judge bias/cost | Shuffle order, anonymize labels, $0 Groq judge; show rationale |
| Scope creep | Phase 2 ordering; MVP = free engines + Gemini session + judge |

---

## 12. Immediate next actions

1. ✅ Handoff brief written: `omnichat-arena/HANDOFF_TO_GEMINI.md` — give it + this plan + the repo to the Gemini agent.
2. Gemini-agent **T0**: compile, install via adb, green on phone (Demo + Pollinations chat).
3. Human pastes Groq key into phone app (Keys tab) → **T2** arena run with verdict.
4. Gemini-agent **T3**: Gemini Pro session (spike-first) → user's Flash 3.8 in-app. 🏆
5. Relay Gemini's per-task reports back here for architecture review; I answer with exact code/edits.

---
*Sources checked 2026-09-14: Duck.ai unofficial API reports; LMArena (no public chat API, leaderboard scrapers only); Perplexity Sonar + Pro pricing; Gemini free tier; xAI Grok API pricing; Claude API pay-per-token (subscription ≠ API credit); DeepSeek pricing. Live tests 2026-09-14: Duck.ai 418 (sandbox + user home); Gemini/DeepSeek endpoint shapes; Groq /models + chat (user key); Pollinations /models + /openai (budget caps).*
