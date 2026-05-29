"""Batch import Bilibili playlists as courses."""
import requests, json, time, sys, os

BASE = "http://localhost:8080/api"

PLAYLISTS = [
    ("https://www.bilibili.com/video/BV1EW411u7th", "CS1"),
    ("https://www.bilibili.com/video/BV1xqdrBeETc", "CS2"),
    ("https://www.bilibili.com/video/BV1nJ411V7bd", "DS1"),
    ("https://www.bilibili.com/video/BV1E4411H73v", "DS2"),
    ("https://www.bilibili.com/video/BV1yGydYEE3H", "WEB"),
    ("https://www.bilibili.com/video/BV17P411m7Ma", "AI"),
    ("https://www.bilibili.com/video/BV1bez8YzEWn", "MATH"),
    ("https://www.bilibili.com/video/BV1X64y1d7mb", "DB"),
    ("https://www.bilibili.com/video/BV1NjExzVEtH", "OS"),
    ("https://www.bilibili.com/video/BV1X8411a7jS", "NET"),
]

def log(msg):
    print(msg, flush=True)

def login():
    resp = requests.post(f"{BASE}/auth/login",
                         json={"username": "teacher", "password": "123456"})
    d = resp.json()
    if not d["success"]:
        log(f"FATAL: Login failed: {d}")
        sys.exit(1)
    return d["data"]["token"]

def headers(token):
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

def parse(url, token):
    resp = requests.post(f"{BASE}/bilibili/playlist",
                         json={"url": url}, headers=headers(token), timeout=60)
    d = resp.json()
    if not d["success"]:
        raise Exception(f"Parse: {d['message']}")
    vids = d["data"]
    return vids[0]["title"], [v["bvid"] for v in vids]

def import_pl(bvids, name, token):
    resp = requests.post(f"{BASE}/bilibili/import-playlist",
                         json={"bvids": bvids, "courseName": name, "autoGenerate": True},
                         headers=headers(token), timeout=None)
    d = resp.json()
    if not d["success"]:
        raise Exception(f"Import: {d.get('message','?')}")
    results = d["data"]["results"]
    ok = sum(1 for r in results if not r.get("error"))
    return ok, len(results) - ok

def main():
    token = login()
    log("Login OK")

    # Parse all
    log("\n=== PHASE 1: PARSE ===")
    parsed = []
    for url, label in PLAYLISTS:
        log(f"[{label}] Parsing...")
        try:
            title, bvids = parse(url, token)
            log(f"[{label}] OK: {len(bvids)} videos")
            parsed.append((label, title, bvids))
        except Exception as e:
            log(f"[{label}] ERROR: {e}")
            parsed.append((label, None, []))

    # Import all
    log("\n=== PHASE 2: IMPORT ===")
    results = []
    t_total = time.time()
    for i, (label, title, bvids) in enumerate(parsed):
        if not bvids:
            log(f"[{i+1}/10] [{label}] SKIP")
            results.append((label, 0, 0, 0))
            continue

        # Refresh token every 3 imports
        if i > 0 and i % 3 == 0:
            token = login()

        log(f"\n[{i+1}/10] [{label}] {title[:60]}")
        log(f"  Videos: {len(bvids)} Start: {time.strftime('%H:%M:%S')}")
        t0 = time.time()
        try:
            ok, fail = import_pl(bvids, title, token)
            elapsed = time.time() - t0
            log(f"  DONE: {ok} OK, {fail} FAIL | {elapsed:.0f}s ({elapsed/60:.1f}m)")
            results.append((label, len(bvids), ok, fail, elapsed))
        except Exception as e:
            elapsed = time.time() - t0
            log(f"  ERROR after {elapsed:.0f}s: {e}")
            results.append((label, len(bvids), 0, len(bvids), elapsed))

    # Summary
    log("\n=== SUMMARY ===")
    total = sum(r[1] for r in results)
    ok_total = sum(r[2] for r in results)
    fail_total = sum(r[3] for r in results)
    for r in results:
        elapsed = f"{r[4]:.0f}s" if len(r) > 4 else "-"
        log(f"  [{r[0]}] {r[1]}v OK:{r[2]} FAIL:{r[3]} ({elapsed})")
    log(f"\nTOTAL: {total} videos, {ok_total} OK, {fail_total} FAIL")
    log(f"Wall time: {(time.time()-t_total)/60:.1f}m")

if __name__ == "__main__":
    main()
