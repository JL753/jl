"""Batch import Bilibili playlists - v2 with error recovery."""
import requests, json, time, sys

BASE = "http://localhost:8080/api"

# Skip CS1,CS2 (already done via previous run: courses 1779182227741, 1779182647590)
# Only import remaining 8 playlists
PLAYLISTS = [
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
                         json={"username": "teacher", "password": "123456"},
                         timeout=10)
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

def import_chunk(bvids, name, token, chunk_idx, total_chunks):
    """Import a chunk of videos. Returns (ok, fail)."""
    chunk_name = name if total_chunks == 1 else f"{name} (Part {chunk_idx}/{total_chunks})"
    log(f"    Chunk {chunk_idx}/{total_chunks}: {len(bvids)} videos, name='{chunk_name[:60]}'")
    t0 = time.time()
    try:
        resp = requests.post(f"{BASE}/bilibili/import-playlist",
                             json={"bvids": bvids, "courseName": chunk_name, "autoGenerate": True},
                             headers=headers(token),
                             timeout=(30, None))  # 30s connect timeout, no read timeout
        d = resp.json()
        if not d["success"]:
            raise Exception(f"Server: {d.get('message','?')}")
        results = d["data"]["results"]
        ok = sum(1 for r in results if not r.get("error"))
        fail = len(results) - ok
        elapsed = time.time() - t0
        log(f"    OK:{ok} FAIL:{fail} Time:{elapsed:.0f}s ({elapsed/60:.1f}m)")
        return ok, fail, True
    except requests.exceptions.Timeout as e:
        elapsed = time.time() - t0
        log(f"    TIMEOUT after {elapsed:.0f}s: {e}")
        return 0, len(bvids), False
    except requests.exceptions.ConnectionError as e:
        elapsed = time.time() - t0
        log(f"    CONNECTION ERROR after {elapsed:.0f}s: {e}")
        return 0, len(bvids), False
    except Exception as e:
        elapsed = time.time() - t0
        log(f"    ERROR after {elapsed:.0f}s: {type(e).__name__}: {e}")
        return 0, len(bvids), False

def main():
    token = login()
    log("Login OK")

    # Parse all remaining playlists
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

    # Import - split large playlists into chunks of 30
    log("\n=== PHASE 2: IMPORT ===")
    CHUNK_SIZE = 30
    results = []
    playlist_num = 1

    for label, title, bvids in parsed:
        if not bvids:
            log(f"\n[{playlist_num}/8] [{label}] SKIP")
            results.append((label, 0, 0, 0))
            playlist_num += 1
            continue

        # Refresh token before each playlist
        token = login()

        n = len(bvids)
        chunks = [bvids[i:i+CHUNK_SIZE] for i in range(0, n, CHUNK_SIZE)]
        total_chunks = len(chunks)

        log(f"\n[{playlist_num}/8] [{label}] {title[:60]}")
        log(f"  Total: {n} videos in {total_chunks} chunk(s) Start: {time.strftime('%H:%M:%S')}")
        t0 = time.time()

        total_ok, total_fail = 0, 0
        for ci, chunk in enumerate(chunks, 1):
            ok, fail, success = import_chunk(chunk, title, token, ci, total_chunks)
            total_ok += ok
            total_fail += fail
            if not success and ci < total_chunks:
                log(f"    Retrying after failure...")
                token = login()
                ok2, fail2, _ = import_chunk(chunk, title, token, ci, total_chunks)
                total_ok = total_ok - ok + ok2
                total_fail = total_fail - fail + fail2

        elapsed = time.time() - t0
        log(f"  TOTAL: {total_ok} OK, {total_fail} FAIL | {elapsed:.0f}s ({elapsed/60:.1f}m)")
        results.append((label, n, total_ok, total_fail, elapsed))
        playlist_num += 1

    # Summary
    log("\n" + "=" * 60)
    log("SUMMARY (v2 run)")
    log("=" * 60)
    total_v, total_ok, total_fail = 0, 0, 0
    for r in results:
        total_v += r[1]; total_ok += r[2]; total_fail += r[3]
        elapsed_str = f"{r[4]:.0f}s" if len(r) > 4 else "-"
        log(f"  [{r[0]}] {r[1]}v OK:{r[2]} FAIL:{r[3]} ({elapsed_str})")
    log(f"\n  TOTAL: {total_v} videos, {total_ok} OK, {total_fail} FAIL")


if __name__ == "__main__":
    main()
