# -*- coding: utf-8 -*-
"""副歌前过渡句（约 64s、69s、148s、153s）再前移 10 秒。"""
import re
import pymysql

SONG_ID = 11
OFFSET = -10.0
# 仅这几行时间落在副歌保护带里但未改过的过渡句
TARGET_RANGES = [(63.9, 64.28), (69.0, 69.5), (148.0, 148.6), (153.5, 154.0)]

TIME_RE = re.compile(r"\[(\d{2}):(\d{2})\.(\d{2,3})\]")


def ts_to_sec(m, s, frac):
    return m * 60 + int(s) + int(frac) / (1000 if len(frac) == 3 else 100)


def sec_to_tag(sec):
    if sec < 0:
        sec = 0.0
    m = int(sec // 60)
    s = sec - m * 60
    whole = int(s)
    frac = int(round((s - whole) * 1000))
    if frac >= 1000:
        whole += 1
        frac = 0
    if whole >= 60:
        m += whole // 60
        whole = whole % 60
    return f"[{m:02d}:{whole:02d}.{frac:03d}]"


def first_ts(line):
    m = TIME_RE.search(line)
    if not m:
        return None
    return ts_to_sec(int(m.group(1)), int(m.group(2)), m.group(3))


def in_target(t):
    return any(lo <= t <= hi for lo, hi in TARGET_RANGES)


def shift_line(line):
    def repl(m):
        t = ts_to_sec(int(m.group(1)), int(m.group(2)), m.group(3)) + OFFSET
        return sec_to_tag(t)
    return TIME_RE.sub(repl, line)


conn = pymysql.connect(host="localhost", user="root", password="123456", database="tp_music", charset="utf8mb4")
cur = conn.cursor()
cur.execute("SELECT lyric FROM song WHERE id = 11")
lines = cur.fetchone()[0].split("\n")
out, n = [], 0
for line in lines:
    t = first_ts(line)
    if t is not None and in_target(t):
        nl = shift_line(line)
        print("OLD:", line)
        print("NEW:", nl)
        out.append(nl)
        n += 1
    else:
        out.append(line)
cur.execute("UPDATE song SET lyric = %s WHERE id = 11", ("\n".join(out),))
conn.commit()
print("bridge lines changed:", n)
conn.close()
