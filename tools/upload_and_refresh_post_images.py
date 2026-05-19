import json
import random
from collections import defaultdict
from pathlib import Path

import pymysql
import requests


IMAGE_DIR = Path(r"F:\毕业设计\封面图")
UPLOAD_API = "http://127.0.0.1:8888/post/image/upload"
MYSQL_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "tp_music",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
    "autocommit": False,
}


def iter_local_images():
    exts = {".jpg", ".jpeg", ".png", ".gif", ".webp"}
    if not IMAGE_DIR.exists():
        raise FileNotFoundError(f"图片目录不存在: {IMAGE_DIR}")
    files = [p for p in IMAGE_DIR.iterdir() if p.is_file() and p.suffix.lower() in exts]
    files.sort(key=lambda p: p.name.lower())
    return files


def upload_images(files):
    uploaded = []
    for f in files:
        with f.open("rb") as fp:
            resp = requests.post(UPLOAD_API, files={"file": (f.name, fp, "image/jpeg")}, timeout=20)
        resp.raise_for_status()
        data = resp.json()
        if not data.get("success"):
            raise RuntimeError(f"上传失败: {f.name} -> {data}")
        uploaded.append(data.get("data"))
    # 去重并保持顺序
    seen = set()
    uniq = []
    for u in uploaded:
        if u and u not in seen:
            seen.add(u)
            uniq.append(u)
    return uniq


def pick_url(urls, idx):
    return urls[idx % len(urls)]


def main():
    files = iter_local_images()
    if not files:
        print("未找到可上传图片，已结束。")
        return

    print(f"待上传图片数量: {len(files)}")
    uploaded_urls = upload_images(files)
    print(f"上传成功数量: {len(uploaded_urls)}")
    if not uploaded_urls:
        print("没有可用的上传结果，已结束。")
        return

    conn = pymysql.connect(**MYSQL_CONFIG)
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, consumer_id, title, cover_url, images, create_time
                FROM post
                WHERE status = 1
                ORDER BY create_time DESC, id DESC
                """
            )
            posts = cur.fetchall()

            by_cover = defaultdict(list)
            for p in posts:
                cover = (p.get("cover_url") or "").strip()
                if cover:
                    by_cover[cover].append(p)

            duplicate_targets = []
            for cover, items in by_cover.items():
                if len(items) <= 1:
                    continue
                # 保留最新一条，替换其余重复封面
                for old in items[1:]:
                    duplicate_targets.append(old)

            no_image_targets = []
            for p in posts:
                cover = (p.get("cover_url") or "").strip()
                images = (p.get("images") or "").strip()
                if not cover and not images:
                    no_image_targets.append(p)

            images_empty_targets = []
            for p in posts:
                images = (p.get("images") or "").strip()
                if not images:
                    images_empty_targets.append(p)

            changed_ids = set()
            idx = random.randint(0, len(uploaded_urls) - 1)

            # 1) 替换重复封面（避免社区刷屏同图）
            for p in duplicate_targets:
                new_url = pick_url(uploaded_urls, idx)
                idx += 1
                cur.execute(
                    """
                    UPDATE post
                    SET cover_url = %s,
                        images = %s,
                        update_time = NOW()
                    WHERE id = %s
                    """,
                    (new_url, json.dumps([new_url], ensure_ascii=False), p["id"]),
                )
                changed_ids.add(p["id"])

            # 2) 给完全无图帖子补图
            for p in no_image_targets:
                if p["id"] in changed_ids:
                    continue
                new_url = pick_url(uploaded_urls, idx)
                idx += 1
                cur.execute(
                    """
                    UPDATE post
                    SET cover_url = %s,
                        images = %s,
                        update_time = NOW()
                    WHERE id = %s
                    """,
                    (new_url, json.dumps([new_url], ensure_ascii=False), p["id"]),
                )
                changed_ids.add(p["id"])

            # 3) 给 images 为空的帖子补上 images（便于列表统一展示）
            for p in images_empty_targets:
                if p["id"] in changed_ids:
                    continue
                cover = (p.get("cover_url") or "").strip()
                new_url = cover if cover else pick_url(uploaded_urls, idx)
                if not cover:
                    idx += 1
                cur.execute(
                    """
                    UPDATE post
                    SET images = %s,
                        update_time = NOW()
                    WHERE id = %s
                    """,
                    (json.dumps([new_url], ensure_ascii=False), p["id"]),
                )
                changed_ids.add(p["id"])

            conn.commit()

            print(f"重复封面替换: {len(duplicate_targets)} 条")
            print(f"无图帖子补图: {len(no_image_targets)} 条")
            print(f"images 字段补全: {len(images_empty_targets)} 条")
            print(f"实际更新帖子: {len(changed_ids)} 条")

    finally:
        conn.close()


if __name__ == "__main__":
    main()
