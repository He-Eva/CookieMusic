import io
import random
import re
import sys

import pymysql


def force_utf8_console():
    try:
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")
    except Exception:
        pass


def classify_playlist(style: str, title: str) -> str:
    text = f"{style or ''} {title or ''}"
    if "粤语" in text:
        return "yueyu"
    if "日韩" in text or "韩" in text or "日" in text:
        return "rihan"
    if "欧美" in text:
        return "oumei"
    if "BGM" in text or "配乐" in text:
        return "bgm"
    if "轻音乐" in text and "乐器" in text:
        return "light_instrument"
    if "轻音乐" in text:
        return "light"
    if "乐器" in text or "钢琴" in text or "吉他" in text:
        return "instrument"
    return "cn"


def build_song_pools(songs):
    # songs: [{id,name}]
    all_ids = [s["id"] for s in songs]

    def contains(name: str, kws):
        n = name or ""
        return any(k in n for k in kws)

    # 粤语
    yueyu_kws = ["陈奕迅", "Beyond", "李克勤", "古巨基", "富士山下", "钟无艳", "吴哥窟", "粤"]
    yueyu = [s["id"] for s in songs if contains(s["name"], yueyu_kws)]

    # 日韩（更严格：韩文/日文字符 + 已知日韩艺人关键词）
    rihan_kws = [
        "IU", "艺声", "梁耀燮", "The S#Arp", "눈물", "리본", "Confession",
        "Here I am", "Paper Umbrella", "Bye Bye Love", "Twilight", "K-POP",
        "日语", "韩语", "OST"
    ]
    jp_kr_char = re.compile(r"[\u3040-\u30ff\uac00-\ud7af]")
    rihan = [s["id"] for s in songs if contains(s["name"], rihan_kws)]
    rihan = list(dict.fromkeys(rihan + [s["id"] for s in songs if s["name"] and jp_kr_char.search(s["name"])]))

    # 欧美（严格：标题首字符 ASCII 且不包含明显中文歌手关键词）
    cn_markers = ["周杰伦", "陈奕迅", "李克勤", "林俊杰", "邓紫棋", "蔡依林", "张杰", "王力宏", "Beyond", "五月天"]
    oumei = [
        s["id"] for s in songs
        if s["name"] and ord(s["name"][0]) < 128 and not contains(s["name"], cn_markers)
    ]

    # BGM/轻音乐/乐器
    bgm_kws = ["BGM", "配乐", "Epic", "Ennio", "Instrumental", "纯音乐", "史诗", "Titoli", "Once Upon"]
    bgm = [s["id"] for s in songs if contains(s["name"], bgm_kws)]

    light_kws = ["纯音乐", "钢琴", "吉他", "配乐", "Ennio", "Instrumental", "Piano"]
    light = [s["id"] for s in songs if contains(s["name"], light_kws)]

    instrument_kws = ["钢琴", "吉他", "小提琴", "古筝", "二胡", "配乐", "Instrumental", "Piano"]
    instrument = [s["id"] for s in songs if contains(s["name"], instrument_kws)]

    cn = [s["id"] for s in songs if s["id"] not in set(oumei)]

    return {
        "all": all_ids,
        "cn": cn or all_ids,
        "yueyu": yueyu or cn or all_ids,
        "rihan": rihan or oumei or all_ids,
        "oumei": oumei or rihan or all_ids,
        "bgm": bgm or light or all_ids,
        "light": light or bgm or all_ids,
        "instrument": instrument or light or all_ids,
        "light_instrument": (list(dict.fromkeys((light or []) + (instrument or []))) or all_ids),
    }


def pick_ids(pool, all_pool, need, seed):
    # deterministic shuffle by seed
    p = list(pool)
    random.Random(seed).shuffle(p)
    chosen = []
    for sid in p:
        if sid not in chosen:
            chosen.append(sid)
        if len(chosen) >= need:
            return chosen
    # fallback from all
    ap = list(all_pool)
    random.Random(seed + 997).shuffle(ap)
    for sid in ap:
        if sid not in chosen:
            chosen.append(sid)
        if len(chosen) >= need:
            return chosen
    return chosen


def main():
    force_utf8_console()
    conn = pymysql.connect(
        host="localhost",
        port=3306,
        user="root",
        password="123456",
        database="tp_music",
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )

    target_per_list = 8
    total_inserted = 0
    total_playlists = 0
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id,name FROM song WHERE status=1 ORDER BY id")
            songs = cur.fetchall()
            pools = build_song_pools(songs)

            cur.execute("SELECT id,title,style FROM song_list ORDER BY id")
            playlists = cur.fetchall()

            for pl in playlists:
                pid = pl["id"]
                bucket = classify_playlist(pl.get("style"), pl.get("title"))
                ids = pick_ids(pools[bucket], pools["all"], target_per_list, seed=pid * 131)

                # 重建该歌单曲目，确保风格一致
                cur.execute("DELETE FROM list_song WHERE song_list_id=%s", (pid,))
                for sid in ids:
                    cur.execute("INSERT INTO list_song(song_id, song_list_id) VALUES(%s,%s)", (sid, pid))
                    total_inserted += 1
                total_playlists += 1

        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    print("done")
    print("playlists_rebuilt=", total_playlists)
    print("list_song_inserted=", total_inserted)


if __name__ == "__main__":
    main()

