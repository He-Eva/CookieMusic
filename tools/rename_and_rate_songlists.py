import io
import random
import sys

import pymysql


def force_utf8_console():
    try:
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")
    except Exception:
        pass


def classify_style(style: str) -> str:
    s = (style or "").strip()
    if "粤语" in s:
        return "yueyu"
    if "日韩" in s:
        return "rihan"
    if "欧美" in s:
        return "oumei"
    if "BGM" in s:
        return "bgm"
    if "轻音乐" in s and "乐器" in s:
        return "light_instrument"
    if "轻音乐" in s:
        return "light"
    if "乐器" in s:
        return "instrument"
    return "cn"


TITLE_POOLS = {
    "cn": [
        "通勤路上单曲循环的华语歌",
        "深夜耳机党私藏华语",
        "下班后最能放松的中文歌",
        "适合一个人散步听的华语",
        "周末宅家听不腻的中文歌",
        "越听越上头的华语旋律",
        "温柔但有力量的华语歌单",
        "最近反复循环的中文歌",
        "写作学习都适合的华语BGM",
        "有故事感的华语宝藏歌",
    ],
    "yueyu": [
        "粤语老友记：耐听不过时",
        "粤语情歌收藏夹",
        "通勤必听粤语男声",
        "夜里很适合循环的粤语",
        "粤语经典与新声混听",
        "粤语歌里的人间烟火",
        "雨天最搭的粤语旋律",
        "一听就有画面的粤语歌单",
    ],
    "oumei": [
        "开车必备欧美节奏组",
        "跑步时超提神的欧美歌",
        "欧美流行热单慢慢听",
        "午后工作专注欧美旋律",
        "欧美女声氛围感歌单",
        "欧美经典与新歌混搭",
        "派对前热身欧美曲库",
        "越听越上头的欧美歌",
    ],
    "bgm": [
        "高能BGM：工作学习双模式",
        "剪辑常用氛围BGM合集",
        "游戏开黑热血BGM",
        "史诗感背景音乐收藏",
        "专注模式常驻BGM",
        "运动训练动力BGM",
        "沉浸式场景配乐歌单",
        "剧情感拉满的BGM",
    ],
    "rihan": [
        "日韩通勤轻快歌单",
        "韩剧感氛围歌收藏",
        "日系温柔旋律清单",
        "韩语女声耐听精选",
        "日韩夜晚放空歌单",
        "日剧OST感情绪歌",
        "K-POP与抒情混合听",
        "日韩宝藏旋律慢慢收",
    ],
    "light": [
        "睡前放松轻音乐",
        "学习写作轻音乐陪伴",
        "午后安静系轻音乐",
        "解压专用纯音乐清单",
        "不打扰思路的轻旋律",
        "夜读必备轻音乐",
        "工作时常驻轻音乐",
        "治愈向轻音乐小合集",
    ],
    "instrument": [
        "钢琴与吉他的日常陪伴",
        "器乐演奏流行精选",
        "纯器乐高质感合集",
        "下雨天很搭的器乐歌单",
        "工作台常驻器乐BGM",
        "安静又耐听的器乐集",
        "钢琴党私藏演奏曲",
        "乐器控循环播放清单",
    ],
    "light_instrument": [
        "钢琴轻音：安静专注时刻",
        "轻器乐：夜晚慢下来",
        "钢琴与轻旋律日常版",
        "写作业常用轻器乐",
        "舒缓系钢琴纯音合集",
        "轻音乐×器乐治愈向",
        "一键进入平静状态",
        "八十八键的温柔陪伴",
    ],
}


def build_title(style_key: str, idx: int) -> str:
    pool = TITLE_POOLS.get(style_key, TITLE_POOLS["cn"])
    base = pool[idx % len(pool)]
    return base if idx < len(pool) else f"{base} Vol.{(idx // len(pool)) + 1}"


def sample_scores(n: int):
    # Realistic distribution: mostly 7-9, with some 6/10
    choices = [6, 7, 8, 9, 10]
    weights = [8, 24, 34, 24, 10]
    return random.choices(choices, weights=weights, k=n)


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

    updated_titles = 0
    upserted_ratings = 0
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id,title,style FROM song_list ORDER BY id")
            playlists = cur.fetchall()

            # Group counters by style for unique naming cadence
            style_counters = {}
            for pl in playlists:
                style_key = classify_style(pl.get("style"))
                i = style_counters.get(style_key, 0)
                new_title = build_title(style_key, i)
                style_counters[style_key] = i + 1
                if new_title != (pl.get("title") or ""):
                    cur.execute("UPDATE song_list SET title=%s WHERE id=%s", (new_title, pl["id"]))
                    updated_titles += 1

            # Rating data
            cur.execute("SELECT id FROM consumer WHERE status=1 ORDER BY id")
            users = [r["id"] for r in cur.fetchall()]
            if len(users) < 10:
                raise RuntimeError("active consumers too few for realistic rating spread")

            for pl in playlists:
                pid = pl["id"]
                # 6~10 raters per playlist
                k = 6 + (pid % 5)
                raters = users[pid % len(users):] + users[:pid % len(users)]
                raters = raters[:k]
                scores = sample_scores(k)
                for uid, sc in zip(raters, scores):
                    cur.execute(
                        """
                        INSERT INTO rank_list(song_list_id, consumer_id, score)
                        VALUES(%s,%s,%s)
                        ON DUPLICATE KEY UPDATE score=VALUES(score)
                        """,
                        (pid, uid, sc),
                    )
                    upserted_ratings += 1

        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    print("done")
    print("updated_titles=", updated_titles)
    print("upserted_ratings=", upserted_ratings)


if __name__ == "__main__":
    main()

