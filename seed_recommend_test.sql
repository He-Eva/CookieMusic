-- 用于验证“播放+收藏+评分”推荐是否生效的造数脚本
-- 数据库：tp_music
-- 涉及表：play_record（播放）、collect（收藏）、rank_list（评分）
--
-- 使用方式（在命令行执行）：
-- mysql -uroot -p123456 -h localhost -P 3306 tp_music < seed_recommend_test.sql

START TRANSACTION;

-- 选择 3 个已存在用户（来自你当前库的样例）
-- consumer_id: 1 / 12 / 16
-- 选择 3 个已存在歌单（来自你当前库 list_song 映射）
-- song_list_id=6  -> songs: 38,39,82,83,84,85
-- song_list_id=19 -> songs: 2,7,53,54,55
-- song_list_id=15 -> songs: 4,7,21,92,93

-- =========================
-- 1) 播放记录 play_record
-- =========================
-- user 1：偏好歌单 6 的 38/39/82（制造强偏好）
INSERT INTO play_record(consumer_id, song_id, play_time, play_seconds, source) VALUES
 (1, 38, DATE_SUB(NOW(), INTERVAL 2 DAY), 210, 0),
 (1, 38, DATE_SUB(NOW(), INTERVAL 1 DAY), 198, 0),
 (1, 39, DATE_SUB(NOW(), INTERVAL 1 DAY), 185, 0),
 (1, 82, DATE_SUB(NOW(), INTERVAL 12 HOUR), 240, 0),
 (1, 82, DATE_SUB(NOW(), INTERVAL  6 HOUR), 233, 0),
 (1, 83, DATE_SUB(NOW(), INTERVAL  5 HOUR),  80, 0);

-- user 12：也听歌单 6（与 user1 有重叠），并扩展到 83/84（制造相似性）
INSERT INTO play_record(consumer_id, song_id, play_time, play_seconds, source) VALUES
 (12, 38, DATE_SUB(NOW(), INTERVAL 3 DAY), 160, 0),
 (12, 83, DATE_SUB(NOW(), INTERVAL 2 DAY), 220, 0),
 (12, 84, DATE_SUB(NOW(), INTERVAL 1 DAY), 205, 0),
 (12, 84, DATE_SUB(NOW(), INTERVAL 10 HOUR), 190, 0),
 (12, 85, DATE_SUB(NOW(), INTERVAL  3 HOUR), 120, 0);

-- user 16：偏好歌单 19（2/7/54），并少量与歌单 6 交叉（制造“可推荐”的桥梁）
INSERT INTO play_record(consumer_id, song_id, play_time, play_seconds, source) VALUES
 (16,  2, DATE_SUB(NOW(), INTERVAL 4 DAY), 210, 0),
 (16,  7, DATE_SUB(NOW(), INTERVAL 3 DAY), 190, 0),
 (16, 54, DATE_SUB(NOW(), INTERVAL 2 DAY), 230, 0),
 (16, 55, DATE_SUB(NOW(), INTERVAL 1 DAY), 180, 0),
 (16, 83, DATE_SUB(NOW(), INTERVAL 10 HOUR),  90, 0);

-- =========================
-- 2) 收藏 collect（type=0 表示歌曲收藏）
-- 注意：collect.create_time 无默认值，必须显式写入
-- =========================
INSERT INTO collect(user_id, type, song_id, song_list_id, create_time) VALUES
 (1,  0, 38, NULL, NOW()),
 (1,  0, 82, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
 (12, 0, 83, NULL, NOW()),
 (12, 0, 84, NULL, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
 (16, 0, 54, NULL, NOW());

-- =========================
-- 3) 评分 rank_list（对歌单评分，推荐侧会展开到歌单内每首歌）
-- =========================
-- user1 强喜欢歌单6，中等喜欢歌单19
INSERT INTO rank_list(song_list_id, consumer_id, score) VALUES
 (6,  1, 9),
 (19, 1, 6)
ON DUPLICATE KEY UPDATE score = VALUES(score);

-- user12 喜欢歌单6，轻度喜欢歌单15
INSERT INTO rank_list(song_list_id, consumer_id, score) VALUES
 (6,  12, 8),
 (15, 12, 5)
ON DUPLICATE KEY UPDATE score = VALUES(score);

-- user16 强喜欢歌单19，并给歌单15 一个低分
INSERT INTO rank_list(song_list_id, consumer_id, score) VALUES
 (19, 16, 9),
 (15, 16, 3)
ON DUPLICATE KEY UPDATE score = VALUES(score);

COMMIT;

