# -*- coding: utf-8 -*-
"""
告白气球 song.id=11：主歌/间奏再前移 10 秒（副歌段已在 fix_lyric_song11.py 中调整过，不再动）。
"""
import re
import pymysql

SONG_ID = 11
OFFSET_SEC = -10.0

# 已校准副歌，勿再偏移
KEEP_RANGES = [
    (64.0, 82.5),    # 第一遍副歌
    (148.5, 166.5),  # 第二遍副歌
    (191.0, 202.5),  # 结尾副歌
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


def should_shift(t: float) -> bool:
    if t < 30.0:
        return False
    for lo, hi in KEEP_RANGES:
        if lo <= t <= hi:
            return False
    return True


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
        if t is not None and should_shift(t):
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
    print(f"Updated song id={sid}, verse/outro lines changed={changed}")
    conn.close()


if __name__ == "__main__":
    main()
