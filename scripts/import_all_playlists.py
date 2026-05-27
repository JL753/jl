"""Batch import Bilibili playlists as courses for 8 subjects."""
import requests
import json
import time
import sys

BASE = "http://localhost:8080/api"
TOKEN = None

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


def login():
    global TOKEN
    resp = requests.post(f"{BASE}/auth/login",
                         json={"username": "teacher", "password": "123456"})
    data = resp.json()
    if data["success"]:
        TOKEN = data["data"]["token"]
        print(f"Logged in as {data['data']['displayName']} ({data['data']['role']})")
    else:
        print(f"Login failed: {data}")
        sys.exit(1)


def auth_headers():
    return {"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"}


def parse_playlist(url):
    resp = requests.post(f"{BASE}/bilibili/playlist",
                         json={"url": url},
                         headers=auth_headers(),
                         timeout=60)
    data = resp.json()
    if not data["success"]:
        raise Exception(f"Parse failed: {data['message']}")
    videos = data["data"]
    title = videos[0]["title"] if videos else "Untitled"
    bvids = [v["bvid"] for v in videos]
    return title, bvids


def import_playlist(bvids, course_name):
    resp = requests.post(f"{BASE}/bilibili/import-playlist",
                         json={
                             "bvids": bvids,
                             "courseName": course_name,
                             "autoGenerate": True
                         },
                         headers=auth_headers(),
                         timeout=None)
    data = resp.json()
    if not data["success"]:
        raise Exception(f"Import failed: {data.get('message', '?')}")
    result = data["data"]
    ok = sum(1 for r in result["results"] if "error" not in r or not r["error"])
    fail = len(result["results"]) - ok
    return ok, fail


def main():
    login()

    # Phase 1: Parse all playlists
    print("\n" + "=" * 60)
    print("PHASE 1: Parsing all playlists...")
    print("=" * 60)
    parsed = []
    for i, (url, label) in enumerate(PLAYLISTS):
        print(f"\n[{i+1}/{len(PLAYLISTS)}] [{label}] Parsing...")
        try:
            title, bvids = parse_playlist(url)
            print(f"  Title: {title}")
            print(f"  Videos: {len(bvids)}")
            parsed.append((label, title, bvids))
        except Exception as e:
            print(f"  ERROR: {e}")
            parsed.append((label, None, []))

    # Phase 2: Import each playlist
    print("\n" + "=" * 60)
    print("PHASE 2: Importing playlists as courses...")
    print("=" * 60)

    results = []
    for i, (label, title, bvids) in enumerate(parsed):
        if not bvids:
            print(f"\n[{i+1}/{len(parsed)}] [{label}] SKIP (no videos)")
            results.append((label, title, 0, 0, 0))
            continue

        print(f"\n[{i+1}/{len(parsed)}] [{label}] Importing: {title}")
        print(f"  Videos: {len(bvids)} | Start: {time.strftime('%H:%M:%S')}")
        t0 = time.time()
        try:
            ok, fail = import_playlist(bvids, title)
            elapsed = time.time() - t0
            print(f"  OK: {ok}, Failed: {fail} | Time: {elapsed:.0f}s ({elapsed/60:.1f}m)")
            results.append((label, title, len(bvids), ok, fail, elapsed))
        except Exception as e:
            elapsed = time.time() - t0
            print(f"  ERROR after {elapsed:.0f}s: {e}")
            results.append((label, title, len(bvids), 0, len(bvids), elapsed))

    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    total_v, total_ok, total_fail = 0, 0, 0
    for r in results:
        label, title, vcount, ok, fail = r[0], r[1], r[2], r[3], r[4]
        elapsed = r[5] if len(r) > 5 else 0
        total_v += vcount
        total_ok += ok
        total_fail += fail
        status = f"OK:{ok}" if fail == 0 else f"OK:{ok} FAIL:{fail}"
        print(f"  [{label}] {vcount}v {status} ({elapsed:.0f}s)")
    print(f"\n  TOTAL: {total_v} videos, {total_ok} OK, {total_fail} Failed")


if __name__ == "__main__":
    main()
