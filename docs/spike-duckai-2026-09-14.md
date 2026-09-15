# Spike: Duck.ai unofficial API — 2026-09-14

**Verdict: ❌ BLOCKED for now — needs a JS-challenge solver, and even public solvers are failing today.**

## What we ran (live, from this sandbox)
1. `GET https://duckduckgo.com/duckchat/v1/status` + `X-Vqd-Accept: 1`
   → `200 {"status":"0","secondaryStatus":"0","statusV2":0}`
   → **No `x-vqd-4` header.** Instead: `x-vqd-hash-1` = 6.5 KB base64 blob.
2. Decoded blob = obfuscated **"DuckDuckGo Fraud & Abuse" JavaScript challenge**
   (reads `navigator`, checks `navigator.webdriver`, builds an iframe, hashes signals).
3. `POST /duckchat/v1/chat` with the blob as token → **HTTP 418 `ERR_CHALLENGE`** on all
   4 models tried, on both `duckduckgo.com` and `duck.ai` hosts.
4. Installed latest public solver **`p2d-duck 1.3.1`** (V8 via mini-racer + DOM stubs,
   the exact recipe from its docs) → **also 418 `ERR_CHALLENGE`** today.

Repro: `python3 spike_duckai.py "Reply with exactly: spike OK"` (stdlib only, in repo root).

## What this means
- The old `status → x-vqd-4 → chat` flow documented in 2024/2025 wrappers is **dead**.
- Current flow: `status → x-vqd-hash-1 (JS challenge) → EXECUTE it in JS+DOM →
  derived token → chat`. See `p2d-duck` (PyPI, MIT) and `benoitpetit/duckduckgo-chat-cli`
  (Go) for reference implementations.
- ⚠️ Even the reference solver fails right now → either the challenge rotated, or
  **datacenter IPs are distrusted**. Our sandbox egress is a datacenter IP; a real
  phone on a residential/mobile network (e.g., Bursa, Turkcell/Turk Telekom) may pass.

## Action items
- [ ] **User:** run `pip install p2d-duck && python3 -c "from duck_ai import DuckChat; print(DuckChat().ask('hi'))"`
      from your home/phone network. If it answers → IP-based blocking, phone is fine.
- [ ] If phone network passes: port the solver to Android = embedded JS runtime
      (**QuickJS-Android** or **J2V8**, ~1–2 MB) + DOM stubs + SHA-256 → token.
      Still 100% native, no WebView. Estimate: 3–5 days.
- [ ] If phone network also 418s: Duck.ai drops to **Phase 2 / stretch**, MVP becomes
      Gemini + DeepSeek (+ Perplexity-Sonar if you have Pro credit).

## MVP order revised
~~Gemini → DeepSeek → Duck.ai~~ → **Gemini → DeepSeek → (Duck.ai iff home-IP check passes)**
→ Perplexity → Claude → Grok → Arena.

## References
- gpt4free PR #2853 (ERR_CHALLENGE header handling, incl. `build_x_vqd_hash_1`)
- aandrew-me/tgpt#347 (full header recipe, Mar 2025 — pre-challenge era)
- p2d-duck 1.3.1 docs (challenge.solve via V8 + stubs.js)
- bownux/duckai-rofi README ("cannot be driven with plain HTTP")
