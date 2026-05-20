# -*- coding: utf-8 -*-
import re
import pymysql

TIME_RE = re.compile(r"\[(\d{2}):(\d{2})\.(\d{2,3})\]")

def ts_to_sec(m, s, frac):
    return m * 60 + int(s) + int(frac) / (1000 if len(frac) == 3 else 100)

conn = pymysql.connect(host="localhost", user="root", password="123456", database="tp_music", charset="utf8mb4")
cur = conn.cursor()
cur.execute("SELECT lyric FROM song WHERE id = 11")
lyric = cur.fetchone()[0]
for line in lyric.split("\n"):
    m = TIME_RE.search(line)
    if m:
        t = ts_to_sec(int(m.group(1)), int(m.group(2)), m.group(3))
        print(f"{t:7.2f}s  {line}")
conn.close()
