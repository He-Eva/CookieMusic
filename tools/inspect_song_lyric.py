# -*- coding: utf-8 -*-
import pymysql

conn = pymysql.connect(
    host="localhost", user="root", password="123456", database="tp_music", charset="utf8mb4"
)
cur = conn.cursor()
cur.execute("SELECT id, name, lyric FROM song WHERE id = 11")
row = cur.fetchone()
lyric = row[2] or ""
for line in lyric.split("\n"):
    if "塞纳" in line or "咖啡" in line or "愿意" in line or "甜蜜" in line:
        print(line)
conn.close()
