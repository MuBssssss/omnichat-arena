# Grok web-session spike

## Results
- **Date**: 2026-09-15
- **Status**: PROBED
- **Findings**:
  - The main page (`https://grok.com/`) loads without immediate Cloudflare bot-challenge blocking for basic requests using a common User-Agent.
  - The API endpoint (`https://grok.com/api/rpc`) returns HTTP 404/401/403 and does not appear to trigger a Cloudflare bot wall (`cf-chl`), though the 404 suggests it's not the correct API route or it strictly requires session authentication cookies (`grok_device_id`, etc.).
  - The main page sets a `grok_device_id` cookie and `__cf_bm` (Cloudflare Bot Management) cookie.
  - The fact that the main page returns a 200 OK and no immediate JavaScript challenges (`ERR_CHALLENGE`) suggests that an automated Custom Tab session capture *might* be feasible, unlike Duck.ai which blocked on the first request.

## Action Plan
- Grok sessions are potentially viable.
- A user login via an in-app Custom Tab to capture cookies could work, provided we find the exact chat API endpoint used by the web app.
