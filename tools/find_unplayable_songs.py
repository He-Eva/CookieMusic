# -*- coding: utf-8 -*-
"""扫描 song 表，通过 HTTP 探测音频是否可播放（需后端 8888 已启动）。"""
import re
import sys
from urllib.parse import quote

import pymysql
import requests

BASE = "http://localhost:8888"
TIMEOUT = 8


def probe_url(full_url):
    try:
        r = requests.head(full_url, timeout=TIMEOUT, allow_redirects=True)
        if r.status_code == 200:
            cl = r.headers.get("Content-Length")
            if cl and int(cl) > 1000:
                return True, r.status_code
            # 部分服务 HEAD 无 length，再 GET 前 4KB
            r2 = requests.get(full_url, timeout=TIMEOUT, stream=True, headers={"Range": "bytes=0-4095"})
            ok = r2.status_code in (200, 206) and len(r2.content) > 500
            return ok, r2.status_code
        return False, r.status_code
    except Exception as e:
        return False, str(e)


def main():
    conn = pymysql.connect(
        host="localhost", port=3306, user="root", password="123456",
        database="tp_music", charset="utf8mb4",
    )
    bad_ids = []
    with conn.cursor() as cur:
        cur.execute(
            "SELECT s.id, s.name, s.url, sg.name FROM song s "
            "LEFT JOIN singer sg ON s.singer_id=sg.id ORDER BY s.id"
        )
        rows = cur.fetchall()
    for sid, name, url, singer in rows:
        reasons = []
        if not url or not str(url).strip():
            reasons.append("empty_url")
        elif not str(url).startswith("/user01/song/music/"):
            reasons.append("bad_url_pattern")
        if singer is None:
            reasons.append("orphan_singer")
        if re.match(r"^(111|测试|test)", str(name or ""), re.I) or str(name or "").startswith("111-"):
            reasons.append("test_name")
        playable = None
        if url and str(url).strip().startswith("/user01"):
            full = BASE + (url if url.startswith("/") else "/" + url)
            playable, code = probe_url(full)
            if not playable:
                reasons.append("http_fail:%s" % code)
        if reasons:
            bad_ids.append((sid, name, url, reasons))
            print(sid, name, reasons)
    print("\nTOTAL_BAD", len(bad_ids))
    print("IDS", [x[0] for x in bad_ids])
    conn.close()
    return [x[0] for x in bad_ids]


if __name__ == "__main__":
    main()
