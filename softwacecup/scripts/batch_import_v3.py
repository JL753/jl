"""Batch import v3 - resume from where v2 left off."""
import requests, json, time, sys

BASE = "http://localhost:8080/api"

# DS1 has 173 total, we already imported 120 (chunks 1-4), remaining 53 (indices 120-172)
# Format: (url, label, start_index, max_to_import) - None start means all
PLAYLISTS = [
    # DS1 remaining: indices 120-172 (53 videos)
    ("https://www.bilibili.com/video/BV1nJ411V7bd", "DS1", 120, 173),
    # Full playlists
    ("https://www.bilibili.com/video/BV1E4411H73v", "DS2", 0, None),
    ("https://www.bilibili.com/video/BV1yGydYEE3H", "WEB", 0, None),
    ("https://www.bilibili.com/video/BV17P411m7Ma", "AI", 0, None),
    ("https://www.bilibili.com/video/BV1bez8YzEWn", "MATH", 0, None),
    ("https://www.bilibili.com/video/BV1X64y1d7mb", "DB", 0, None),
    ("https://www.bilibili.com/video/BV1NjExzVEtH", "OS", 0, None),
    ("https://www.bilibili.com/video/BV1X8411a7jS", "NET", 0, None),
]

def log(msg):
    print(msg, flush=True)

def login():
    resp = requests.post(f"{BASE}/auth/login",
                         json={"username": "teacher", "password": "123456"},
                         timeout=10)
    d = resp.json()
    return d["data"]["token"]

def hdr(token):
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

def parse(url, token):
    resp = requests.post(f"{BASE}/bilibili/playlist",
                         json={"url": url}, headers=hdr(token), timeout=60)
    d = resp.json()
    if not d["success"]:
        raise Exception(f"Parse: {d['message']}")
    return d["data"][0]["title"], [v["bvid"] for v in d["data"]]

def import_chunk(bvids, name, token, part_no, total_parts):
    chunk_name = name if total_parts == 1 else f"{name} (Part {part_no}/{total_parts})"
    log(f"  Part {part_no}/{total_parts}: {len(bvids)} videos")
    t0 = time.time()
    try:
        resp = requests.post(f"{BASE}/bilibili/import-playlist",
                             json={"bvids": bvids, "courseName": chunk_name, "autoGenerate": True},
                             headers=hdr(token),
                             timeout=(30, 1800))  # 30s connect, 30min read timeout
        d = resp.json()
        if not d["success"]:
            raise Exception(f"Server: {d.get('message','?')}")
        ok = sum(1 for r in d["data"]["results"] if not r.get("error"))
        fail = len(d["data"]["results"]) - ok
        elapsed = time.time() - t0
        log(f"  OK:{ok} FAIL:{fail} Time:{elapsed:.0f}s ({elapsed/60:.1f}m)")
        return ok, fail, True
    except Exception as e:
        elapsed = time.time() - t0
        log(f"  ERROR after {elapsed:.0f}s: {type(e).__name__}: {str(e)[:100]}")
        return 0, len(bvids), False

def main():
    token = login()
    log("Login OK")

    # Parse only what we need
    log("\n=== PARSE ===")
    parsed = []
    for url, label, start_idx, end_idx in PLAYLISTS:
        log(f"[{label}] Parsing...")
        try:
            title, all_bvids = parse(url, token)
            if start_idx > 0:
                bvids = all_bvids[start_idx:]
                log(f"[{label}] {len(all_bvids)} total, resuming from #{start_idx}, {len(bvids)} remaining")
            else:
                bvids = all_bvids
                log(f"[{label}] OK: {len(bvids)} videos")
            parsed.append((label, title, bvids, start_idx))
        except Exception as e:
            log(f"[{label}] ERROR: {e}")
            parsed.append((label, None, [], 0))

    # Import - split into chunks of 25 (smaller for stability)
    log("\n=== IMPORT ===")
    CHUNK = 25
    results = []
    pnum = 1
    total_pl = len(parsed)

    for label, title, bvids, start_idx in parsed:
        if not bvids:
            log(f"\n[{pnum}/{total_pl}] [{label}] SKIP")
            results.append((label, 0, 0, 0))
            pnum += 1
            continue

        token = login()
        n = len(bvids)
        chunks = [bvids[i:i+CHUNK] for i in range(0, n, CHUNK)]
        tc = len(chunks)

        # Adjust part numbering for resumed playlist
        offset = (start_idx // CHUNK) + 1 if start_idx > 0 else 1

        log(f"\n[{pnum}/{total_pl}] [{label}] {title[:60]}")
        log(f"  Total: {n} videos in {tc} chunk(s) Start: {time.strftime('%H:%M:%S')}")
        t0 = time.time()

        total_ok, total_fail = 0, 0
        for ci, chunk in enumerate(chunks):
            ok, fail, success = import_chunk(chunk, title, token, offset + ci, offset + tc - 1)
            total_ok += ok
            total_fail += fail
            if not success:
                log(f"  !!! Chunk failed, continuing to next...")
                token = login()

        elapsed = time.time() - t0
        log(f"  TOTAL: {total_ok} OK, {total_fail} FAIL | {elapsed:.0f}s ({elapsed/60:.1f}m)")
        results.append((label, n, total_ok, total_fail))
        pnum += 1

    # Summary
    log("\n=== FINAL SUMMARY ===")
    total_v, total_ok, total_fail = 0, 0, 0
    for r in results:
        total_v += r[1]; total_ok += r[2]; total_fail += r[3]
        log(f"  [{r[0]}] {r[1]}v OK:{r[2]} FAIL:{r[3]}")
    log(f"\n  TOTAL: {total_v} videos, {total_ok} OK, {total_fail} FAIL")

if __name__ == "__main__":
    main()
