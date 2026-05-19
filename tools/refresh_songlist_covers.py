from pathlib import Path

import pymysql
import requests


IMAGE_DIR = Path(r"F:\毕业设计\封面图")
UPLOAD_API_URL = "http://127.0.0.1:8888/post/image/upload"

MYSQL_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "tp_music",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
}


def list_images():
    exts = {".jpg", ".jpeg", ".png", ".gif", ".webp"}
    images = [p for p in IMAGE_DIR.iterdir() if p.is_file() and p.suffix.lower() in exts]
    images.sort(key=lambda x: x.name.lower())
    return images


def load_songlist_ids():
    conn = pymysql.connect(**MYSQL_CONFIG)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM song_list WHERE status = 1 ORDER BY id ASC")
            return [row["id"] for row in cur.fetchall()]
    finally:
        conn.close()


def upload_images(images):
    uploaded_urls = []
    for img in images:
        with img.open("rb") as f:
            files = {"file": (img.name, f, "image/jpeg")}
            resp = requests.post(UPLOAD_API_URL, files=files, timeout=30)
        if resp.status_code != 200:
            raise RuntimeError(f"上传失败({resp.status_code}): {img.name}")
        body = resp.json()
        if not body.get("success"):
            raise RuntimeError(f"上传失败: {img.name} -> {body}")
        uploaded_urls.append(body.get("data"))
    uniq = []
    seen = set()
    for url in uploaded_urls:
        if url and url not in seen:
            seen.add(url)
            uniq.append(url)
    return uniq


def main():
    if not IMAGE_DIR.exists():
        raise FileNotFoundError(f"目录不存在: {IMAGE_DIR}")

    images = list_images()
    if not images:
        raise RuntimeError("未找到可用图片")

    songlist_ids = load_songlist_ids()
    if not songlist_ids:
        print("没有可更新的上架歌单")
        return

    uploaded_urls = upload_images(images)
    if not uploaded_urls:
        raise RuntimeError("上传完成但没有可用 URL")

    conn = pymysql.connect(**MYSQL_CONFIG)
    updated = 0
    try:
        with conn.cursor() as cur:
            for idx, sid in enumerate(songlist_ids):
                url = uploaded_urls[idx % len(uploaded_urls)]
                cur.execute("UPDATE song_list SET pic=%s WHERE id=%s", (url, sid))
                updated += cur.rowcount
        conn.commit()
    finally:
        conn.close()

    print(f"总歌单数: {len(songlist_ids)}")
    print(f"上传图片数: {len(uploaded_urls)}")
    print(f"更新成功: {updated}")


if __name__ == "__main__":
    main()
