import io
import random
import sys
from datetime import datetime, timedelta

import pymysql


def force_utf8_console():
    try:
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")
    except Exception:
        pass


def intro_for_style(title: str, style: str) -> str:
    s = (style or "").strip()
    if "粤语" in s:
        return f"这份《{title}》偏向通勤和夜晚反复听的粤语歌，旋律耐听、歌词有画面感。适合一个人散步、开车或加班后放松时循环播放。"
    if "日韩" in s:
        return f"《{title}》整理了情绪感和氛围感都很强的日韩歌曲，适合学习、发呆、晚间放空。希望你在不同情绪里都能找到一首刚好懂你的歌。"
    if "欧美" in s:
        return f"《{title}》以节奏感和旋律辨识度高的欧美歌曲为主，适合运动、开车和效率场景。既能提神，也能在放松时保持好心情。"
    if "BGM" in s:
        return f"《{title}》主打氛围型背景音乐，适配写作、剪辑、游戏和专注工作场景。整套歌单尽量保证情绪连贯，不突兀、可长时间播放。"
    if "轻音乐" in s or "乐器" in s:
        return f"《{title}》以轻音乐/器乐为主，适合阅读、学习和睡前放松。旋律干净耐听，不抢注意力，适合作为日常陪伴型歌单。"
    return f"《{title}》按日常使用场景做了重新整理，覆盖通勤、学习、夜晚放松等时段。歌曲风格尽量保持一致，方便直接点击顺序播放。"


def pick_pool(style: str, title: str, pools: dict):
    text = f"{style or ''} {title or ''}"
    if "粤语" in text:
        return pools["yueyu"] or pools["cn"] or pools["all"]
    if "日韩" in text:
        return pools["rihan"] or pools["all"]
    if "欧美" in text:
        return pools["oumei"] or pools["all"]
    if "BGM" in text or "轻音乐" in text or "乐器" in text:
        return pools["light"] or pools["all"]
    return pools["cn"] or pools["all"]


def build_comment(style: str, title: str) -> str:
    templates = [
        "这套《{title}》比我想的更耐听，顺序播放体验很好。",
        "今天通勤全程在听《{title}》，节奏和情绪都很舒服。",
        "这个歌单分类很实用，收藏了，之后会常来听。",
        "晚上写东西的时候放《{title}》刚刚好，不会打断思路。",
        "这份《{title}》里有几首直接加到我的常听列表了。",
        "风格挺统一的，听下来很顺，不会突然跳戏。",
    ]
    base = random.choice(templates).replace("{title}", title)
    s = style or ""
    if "粤语" in s:
        return base + " 粤语控表示很满意。"
    if "日韩" in s:
        return base + " 日韩氛围感拉满。"
    if "欧美" in s:
        return base + " 欧美节奏感很在线。"
    if "BGM" in s or "轻音乐" in s:
        return base + " 当背景音乐真的很稳。"
    return base


def main():
    force_utf8_console()
    random.seed(20260416)

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

    target_song_count = 8
    target_comment_count = 3
    inserted_list_song = 0
    updated_intro = 0
    inserted_comment = 0

    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id,name FROM song WHERE status=1 ORDER BY id")
            songs = cur.fetchall()
            all_ids = [x["id"] for x in songs]

            # Heuristic pools for realistic style matching
            def is_ascii_char(ch: str) -> bool:
                return ord(ch) < 128

            oumei_ids = [x["id"] for x in songs if x["name"] and (is_ascii_char(x["name"][0]) or "Ennio" in x["name"])]
            yueyu_keywords = ["陈奕迅", "Beyond", "李克勤", "古巨基", "钟无艳", "吴哥窟", "富士山下"]
            yueyu_ids = [x["id"] for x in songs if any(k in (x["name"] or "") for k in yueyu_keywords)]
            rihan_keywords = ["IU", "艺声", "梁耀燮", "韩", "日", "樱花", "J.Fla", "飞轮海", "S.H.E", "Tank"]
            rihan_ids = [x["id"] for x in songs if any(k in (x["name"] or "") for k in rihan_keywords)]
            light_keywords = ["钢琴", "BGM", "纯音乐", "配乐", "Instrumental", "Ennio"]
            light_ids = [x["id"] for x in songs if any(k in (x["name"] or "") for k in light_keywords)]
            cn_ids = [x["id"] for x in songs if x["id"] not in set(oumei_ids)]

            pools = {
                "all": all_ids,
                "oumei": oumei_ids,
                "yueyu": yueyu_ids,
                "rihan": rihan_ids,
                "light": light_ids,
                "cn": cn_ids,
            }

            cur.execute("SELECT id,username FROM consumer WHERE status=1 ORDER BY id")
            users = cur.fetchall()
            user_ids = [u["id"] for u in users] or [1]

            cur.execute(
                """
                SELECT sl.id, sl.title, sl.style, sl.introduction, COUNT(ls.song_id) AS song_count
                FROM song_list sl
                LEFT JOIN list_song ls ON sl.id = ls.song_list_id
                GROUP BY sl.id, sl.title, sl.style, sl.introduction
                ORDER BY sl.id
                """
            )
            playlists = cur.fetchall()

            for pl in playlists:
                pid = pl["id"]
                title = pl["title"] or f"歌单{pid}"
                style = pl["style"] or "华语"
                current_song_count = int(pl["song_count"] or 0)

                # 1) Fill songs up to target count
                need = max(0, target_song_count - current_song_count)
                if need > 0:
                    cur.execute("SELECT song_id FROM list_song WHERE song_list_id=%s", (pid,))
                    existing = {r["song_id"] for r in cur.fetchall()}
                    pool = pick_pool(style, title, pools)
                    ordered_pool = pool[pid % max(1, len(pool)):] + pool[:pid % max(1, len(pool))]
                    added = 0
                    for sid in ordered_pool:
                        if sid in existing:
                            continue
                        cur.execute("INSERT INTO list_song(song_id, song_list_id) VALUES(%s,%s)", (sid, pid))
                        existing.add(sid)
                        added += 1
                        inserted_list_song += 1
                        if added >= need:
                            break

                    # fallback: if style pool is not enough, fill from global pool
                    if added < need:
                        all_pool = pools["all"]
                        ordered_all = all_pool[pid % max(1, len(all_pool)):] + all_pool[:pid % max(1, len(all_pool))]
                        for sid in ordered_all:
                            if sid in existing:
                                continue
                            cur.execute("INSERT INTO list_song(song_id, song_list_id) VALUES(%s,%s)", (sid, pid))
                            existing.add(sid)
                            added += 1
                            inserted_list_song += 1
                            if added >= need:
                                break

                # 2) Improve intro if too short/generic
                intro = (pl["introduction"] or "").strip()
                generic = intro in {"", "歌单", "11"} or len(intro) < 12
                if generic:
                    new_intro = intro_for_style(title, style)
                    cur.execute("UPDATE song_list SET introduction=%s WHERE id=%s", (new_intro, pid))
                    updated_intro += 1

                # 3) Fill comments up to target per playlist
                cur.execute("SELECT COUNT(*) AS c FROM comment WHERE type=1 AND song_list_id=%s", (pid,))
                c = int(cur.fetchone()["c"])
                need_c = max(0, target_comment_count - c)
                for i in range(need_c):
                    uid = user_ids[(pid + i) % len(user_ids)]
                    content = build_comment(style, title)
                    ct = datetime.now() - timedelta(days=random.randint(0, 35), minutes=random.randint(0, 1440))
                    cur.execute(
                        """
                        INSERT INTO comment(user_id, song_id, song_list_id, content, create_time, type, `up`)
                        VALUES(%s, NULL, %s, %s, %s, 1, %s)
                        """,
                        (uid, pid, content, ct.strftime("%Y-%m-%d %H:%M:%S"), random.randint(0, 18)),
                    )
                    inserted_comment += 1

        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    print("done")
    print("inserted_list_song=", inserted_list_song)
    print("updated_intro=", updated_intro)
    print("inserted_comment=", inserted_comment)


if __name__ == "__main__":
    main()

