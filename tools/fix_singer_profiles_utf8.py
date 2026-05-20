# -*- coding: utf-8 -*-
"""以 UTF-8 修复 singer 表中 location / introduction 乱码（?）。"""
import pymysql

ROWS = [
    (354, "中国内地",
     "中国内地男子演唱组合，由井柏然、付辛博组成。2007年参加《加油！好男儿》后签约华谊兄弟并出道，代表作《光荣》《恋爱新手》等。"),
    (355, "新加坡",
     "新加坡女子演唱组合，由双胞胎姐妹白纬芬（Miko）、白纬玲（Yumi）组成。2008年发行首张专辑《16未成年》出道，代表曲《爱丫爱丫》《我知道》等。"),
    (356, "中国台湾",
     "台湾流行摇滚乐团，2002年成立，2004年以同名专辑及《Lydia》走红。团名取自成员英文名首字母，亦为 Fairyland In Reality 缩写。"),
    (357, "中国台湾",
     "台湾男子偶像组合，2001年因主演《流星花园》成立，成员言承旭、周渝民、吴建豪、朱孝天，代表曲《流星雨》等。"),
    (358, "中国台湾",
     "台湾创作型兄妹组合，陈忠义、陈绮萱组成，以物语式情歌见长。2004年发行《遇见未来》重新出发，代表作《杀破狼》《Say Forever》等。"),
    (360, "中国广东",
     "中国内地女歌手，本名王麟。2006年以单曲《QQ爱》走红，获网络流行金曲等奖项，后亦有《伤不起》等代表作。"),
    (361, "中国台湾",
     "台湾女子演唱组合，任家萱（Selina）、田馥甄（Hebe）、陈嘉桦（Ella）组成。2001年出道，华语乐坛具代表性的女子天团之一。"),
    (362, "中国台湾",
     "台湾女子双人演唱组合，刘品言、曾之乔组成，2003年出道，风格甜美活泼，代表作《樱花草》《彩虹眼泪》等。"),
    (363, "中国台湾",
     "台湾创作男歌手，本名吕建忠。2005年出道，嗓音沙哑嘹亮，融合摇滚、R&B与戏曲元素，代表作《三国恋》《千年泪》《专属天使》等。"),
    (366, "中国浙江",
     "中国内地女歌手，本名冯沁苑，艺名「买辣椒也用券」。2017年翻唱并发行《起风了》走红，现为独立音乐人。"),
    (367, "马来西亚",
     "马来西亚华裔创作歌手，本名王光良。曾与品冠组成无印良品，2000年后个人发展，代表作《童话》《第一次》《约定》等。"),
    (368, "中国四川",
     "中国内地男歌手，本名罗林。2004年以《2002年的第一场雪》走红，嗓音沧桑独特，代表作还有《西海情歌》《冲动的惩罚》等。"),
]


def main():
    conn = pymysql.connect(
        host="localhost", port=3306, user="root", password="123456",
        database="tp_music", charset="utf8mb4",
        use_unicode=True,
    )
    try:
        with conn.cursor() as cur:
            for sid, location, intro in ROWS:
                cur.execute(
                    "UPDATE singer SET location=%s, introduction=%s WHERE id=%s",
                    (location, intro, sid),
                )
        conn.commit()
        print("updated %d singers" % len(ROWS))
        with conn.cursor() as cur:
            cur.execute(
                "SELECT id, name, location, LEFT(introduction, 40) FROM singer "
                "WHERE id IN (%s) ORDER BY id"
                % ",".join(str(r[0]) for r in ROWS)
            )
            for row in cur.fetchall():
                print(row)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
