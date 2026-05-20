# -*- coding: utf-8 -*-
"""校准 song id=11（告白气球）副歌 LRC 时间轴，前移 10 秒。"""
import re
import pymysql

SONG_ID = 11
OFFSET_SEC = -10.0

# 副歌时间段（秒）：第一遍 01:14~01:32，第二遍 02:39~02:56，尾声 03:21~03:32
SHIFT_RANGES = [
    (74.0, 92.5),
    (159.0, 177.0),
    (201.0, 213.0),
]

TIME_RE = re.compile(r"\[(\d{2}):(\d{2})\.(\d{2,3})\]")


def ts_to_sec(m: int, s: int, frac: str) -> float:
    return m * 60 + int(s) + int(frac) / (1000 if len(frac) == 3 else 100)


def sec_to_tag(sec: float) -> str:
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


def first_ts_sec(line: str):
    m = TIME_RE.search(line)
    if not m:
        return None
    return ts_to_sec(int(m.group(1)), int(m.group(2)), m.group(3))


def in_shift_range(t: float) -> bool:
    return any(lo <= t <= hi for lo, hi in SHIFT_RANGES)


def shift_line(line: str, offset: float) -> str:
    def repl(m):
        t = ts_to_sec(int(m.group(1)), int(m.group(2)), m.group(3)) + offset
        return sec_to_tag(t)

    return TIME_RE.sub(repl, line)


def main():
    conn = pymysql.connect(
        host="localhost",
        user="root",
        password="123456",
        database="tp_music",
        charset="utf8mb4",
    )
    cur = conn.cursor()
    cur.execute("SELECT id, name, lyric FROM song WHERE id = %s", (SONG_ID,))
    row = cur.fetchone()
    if not row:
        print("song not found")
        return
    sid, name, lyric = row
    lines = (lyric or "").split("\n")
    new_lines = []
    changed = 0
    for line in lines:
        t = first_ts_sec(line)
        if t is not None and in_shift_range(t):
            new_line = shift_line(line, OFFSET_SEC)
            if new_line != line:
                changed += 1
                print("OLD:", line)
                print("NEW:", new_line)
            new_lines.append(new_line)
        else:
            new_lines.append(line)
    new_lyric = "\n".join(new_lines)
    cur.execute("UPDATE song SET lyric = %s WHERE id = %s", (new_lyric, sid))
    conn.commit()
    print(f"Updated song id={sid} name={name}, lines changed={changed}")
    conn.close()


if __name__ == "__main__":
    main()
