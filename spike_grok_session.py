import urllib.request
import urllib.error
import json
import uuid
import os

def check_grok():
    print("Checking grok.com...")

    # 1. Try to fetch the homepage or API without a session
    url = "https://grok.com/"
    req = urllib.request.Request(url, headers={
        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:130.0) Gecko/20100101 Firefox/130.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
    })

    device_id = None
    try:
        resp = urllib.request.urlopen(req)
        print("Status:", resp.status)
        cookie_header = resp.getheader('Set-Cookie')

        # Check if they serve Cloudflare challenge or main app
        html = resp.read().decode('utf-8')
        if "cf-chl-widget" in html or "cf-mitigated" in html:
            print("Detected Cloudflare challenge on main page.")
        else:
            print("Main page loaded successfully.")
    except urllib.error.HTTPError as e:
        print("HTTP Error on main page:", e.code)

    print("\nProbing API endpoints...")
    # Try a known or likely endpoint if possible, or just note session requirement
    api_url = "https://grok.com/api/rpc" # Wild guess based on common patterns, just to see what kind of error we get

    try:
        req_api = urllib.request.Request(api_url, headers={
             "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:130.0) Gecko/20100101 Firefox/130.0",
        }, method="POST")
        resp_api = urllib.request.urlopen(req_api)
        print("API Status:", resp_api.status)
    except urllib.error.HTTPError as e:
        print(f"API HTTP Error ({api_url}):", e.code)
        html = e.read().decode('utf-8')
        if "cf-chl" in html or "cloudflare" in html.lower():
            print("Cloudflare blocked API access.")
        elif e.code in [401, 403]:
             print("API returned auth error (expected if no session).")

check_grok()
