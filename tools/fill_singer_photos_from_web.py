import re
import uuid
from pathlib import Path
from urllib.parse import quote

import pymysql
import requests


MYSQL_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "tp_music",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
}

SINGER_UPDATE_API = "http://127.0.0.1:8888/singer/avatar/update"
TMP_DIR = Path("f:/毕业设计/源码/CookieMusicDemo/tools/.tmp_singer_photos")


def normalize_name(name: str) -> str:
    if not name:
        return ""
    s = name.strip()
    s = re.split(r"[，,、/&|]", s)[0].strip()
    s = re.sub(r"\s+", " ", s)
    return s


def get_missing_singers():
    conn = pymysql.connect(**MYSQL_CONFIG)
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, name, pic
                FROM singer
                WHERE pic IS NULL OR pic = '' OR pic LIKE '%default.jpg'
                ORDER BY id ASC
                """
            )
            return cur.fetchall()
    finally:
        conn.close()


def search_deezer_image(artist_name: str):
    # Deezer artist search does not require API key.
    url = f"https://api.deezer.com/search/artist?q={quote(artist_name)}"
    resp = requests.get(url, timeout=12)
    if resp.status_code != 200:
        return None
    data = resp.json().get("data") or []
    if not data:
        return None

    name_l = artist_name.lower()
    exact = None
    for item in data:
        n = str(item.get("name", "")).lower()
        if n == name_l:
            exact = item
            break
    pick = exact or data[0]
    return pick.get("picture_xl") or pick.get("picture_big") or pick.get("picture_medium")


def search_itunes_image(artist_name: str):
    # Fallback: use song cover to represent singer.
    url = (
        "https://itunes.apple.com/search"
        f"?term={quote(artist_name)}&entity=song&limit=20&country=CN"
    )
    resp = requests.get(url, timeout=12)
    if resp.status_code != 200:
        return None
    results = resp.json().get("results") or []
    if not results:
        return None

    name_l = artist_name.lower()
    preferred = None
    for item in results:
        artist = str(item.get("artistName", "")).lower()
        if name_l in artist or artist in name_l:
            preferred = item
            break
    pick = preferred or results[0]
    return pick.get("artworkUrl100") or pick.get("artworkUrl60") or pick.get("artworkUrl30")


def download_image(url: str, singer_id: int):
    resp = requests.get(url, timeout=20)
    if resp.status_code != 200 or not resp.content:
        return None
    content_type = resp.headers.get("Content-Type", "").lower()
    ext = ".jpg"
    if "png" in content_type:
        ext = ".png"
    elif "webp" in content_type:
        ext = ".webp"
    elif "gif" in content_type:
        ext = ".gif"

    TMP_DIR.mkdir(parents=True, exist_ok=True)
    filename = f"singer_{singer_id}_{uuid.uuid4().hex[:10]}{ext}"
    path = TMP_DIR / filename
    path.write_bytes(resp.content)
    return path


def upload_to_backend(path: Path, singer_id: int):
    with path.open("rb") as f:
        files = {"file": (path.name, f, "application/octet-stream")}
        data = {"id": str(singer_id)}
        resp = requests.post(SINGER_UPDATE_API, files=files, data=data, timeout=20)
    if resp.status_code != 200:
        return False, f"http_{resp.status_code}"
    body = resp.json()
    if not body.get("success"):
        return False, body.get("message", "api_failed")
    return True, body.get("data", "")


def main():
    singers = get_missing_singers()
    print(f"缺图歌手总数: {len(singers)}")
    if not singers:
        return

    ok = []
    failed = []

    for s in singers:
        sid = s["id"]
        raw_name = s["name"] or ""
        keyword = normalize_name(raw_name)
        if not keyword:
            failed.append((sid, raw_name, "empty_name"))
            continue

        candidates = []
        try:
            u1 = search_deezer_image(keyword)
            if u1:
                candidates.append(("deezer", u1))
        except Exception:
            pass
        try:
            u2 = search_itunes_image(keyword)
            if u2:
                candidates.append(("itunes", u2))
        except Exception:
            pass

        if not candidates:
            failed.append((sid, raw_name, "no_image_found"))
            continue

        uploaded = False
        last_err = "unknown"
        for source, img_url in candidates:
            try:
                local = download_image(img_url, sid)
                if not local:
                    last_err = f"download_failed:{source}"
                    continue
                success, info = upload_to_backend(local, sid)
                if success:
                    ok.append((sid, raw_name, source, info))
                    uploaded = True
                    break
                last_err = f"upload_failed:{info}"
            except Exception as e:
                last_err = f"exception:{e}"
                continue

        if not uploaded:
            failed.append((sid, raw_name, last_err))

    print(f"补图成功: {len(ok)}")
    print(f"补图失败: {len(failed)}")
    if ok:
        print("成功示例(前10):")
        for item in ok[:10]:
            print(item)
    if failed:
        print("失败示例(前20):")
        for item in failed[:20]:
            print(item)


if __name__ == "__main__":
    main()
