SET NAMES utf8mb4;

-- 补全歌手资料（来源：维基百科、百度百科等公开资料，2026-05 整理）
-- 注意：Windows PowerShell 管道执行可能导致中文变成 ?，请用下面任一方式执行：
--   python tools/fix_singer_profiles_utf8.py
--   mysql --default-character-set=utf8mb4 -uroot -p tp_music < sql/015_enrich_singer_profiles.sql  （CMD 重定向，勿用 Get-Content 管道）
-- sex: 0女 1男 2组合/乐队；组合 birth 取出道或成立日期

UPDATE singer SET
  sex = 2,
  birth = '2007-08-31 00:00:00',
  location = '中国内地',
  introduction = '中国内地男子演唱组合，由井柏然、付辛博组成。2007年参加《加油！好男儿》后签约华谊兄弟并出道，代表作《光荣》《恋爱新手》等。'
WHERE id = 354;

UPDATE singer SET
  sex = 2,
  birth = '2008-07-25 00:00:00',
  location = '新加坡',
  introduction = '新加坡女子演唱组合，由双胞胎姐妹白纬芬（Miko）、白纬玲（Yumi）组成。2008年发行首张专辑《16未成年》出道，代表曲《爱丫爱丫》《我知道》等。'
WHERE id = 355;

UPDATE singer SET
  sex = 2,
  birth = '2004-04-23 00:00:00',
  location = '中国台湾',
  introduction = '台湾流行摇滚乐团，2002年成立，2004年以同名专辑及《Lydia》走红。团名取自成员英文名首字母，亦为 Fairyland In Reality 缩写。'
WHERE id = 356;

UPDATE singer SET
  sex = 2,
  birth = '2001-01-01 00:00:00',
  location = '中国台湾',
  introduction = '台湾男子偶像组合，2001年因主演《流星花园》成立，成员言承旭、周渝民、吴建豪、朱孝天，代表曲《流星雨》等。'
WHERE id = 357;

UPDATE singer SET
  sex = 2,
  birth = '2004-08-18 00:00:00',
  location = '中国台湾',
  introduction = '台湾创作型兄妹组合，陈忠义、陈绮萱组成，以物语式情歌见长。2004年发行《遇见未来》重新出发，代表作《杀破狼》《Say Forever》等。'
WHERE id = 358;

-- id=360 库中名为「QQ爱」，实为歌手王麟（代表作《QQ爱》），资料按王麟填写
UPDATE singer SET
  sex = 0,
  birth = '1983-09-26 00:00:00',
  location = '中国广东',
  introduction = '中国内地女歌手，本名王麟。2006年以单曲《QQ爱》走红，获网络流行金曲等奖项，后亦有《伤不起》等代表作。'
WHERE id = 360;

UPDATE singer SET
  sex = 2,
  birth = '2001-09-11 00:00:00',
  location = '中国台湾',
  introduction = '台湾女子演唱组合，任家萱（Selina）、田馥甄（Hebe）、陈嘉桦（Ella）组成。2001年出道，华语乐坛具代表性的女子天团之一。'
WHERE id = 361;

UPDATE singer SET
  sex = 2,
  birth = '2003-08-01 00:00:00',
  location = '中国台湾',
  introduction = '台湾女子双人演唱组合，刘品言、曾之乔组成，2003年出道，风格甜美活泼，代表作《樱花草》《彩虹眼泪》等。'
WHERE id = 362;

UPDATE singer SET
  sex = 1,
  birth = '1982-02-06 00:00:00',
  location = '中国台湾',
  introduction = '台湾创作男歌手，本名吕建忠。2005年出道，嗓音沙哑嘹亮，融合摇滚、R&B与戏曲元素，代表作《三国恋》《千年泪》《专属天使》等。'
WHERE id = 363;

UPDATE singer SET
  sex = 0,
  birth = '1997-08-28 00:00:00',
  location = '中国浙江',
  introduction = '中国内地女歌手，本名冯沁苑，艺名「买辣椒也用券」。2017年翻唱并发行《起风了》走红，现为独立音乐人。'
WHERE id = 366;

UPDATE singer SET
  sex = 1,
  birth = '1970-08-30 00:00:00',
  location = '马来西亚',
  introduction = '马来西亚华裔创作歌手，本名王光良。曾与品冠组成无印良品，2000年后个人发展，代表作《童话》《第一次》《约定》等。'
WHERE id = 367;

UPDATE singer SET
  sex = 1,
  birth = '1971-06-22 00:00:00',
  location = '中国四川',
  introduction = '中国内地男歌手，本名罗林。2004年以《2002年的第一场雪》走红，嗓音沧桑独特，代表作还有《西海情歌》《冲动的惩罚》等。'
WHERE id = 368;

SELECT id, name, sex, birth, location, LEFT(introduction, 60) AS intro_preview
FROM singer
WHERE id IN (354,355,356,357,358,360,361,362,363,366,367,368)
ORDER BY id;
