-- 演示数据增强脚本（可重复执行）
-- 目标用户：我不想上班但想听歌
-- 用途：补齐“社交中心 / 笔记社区 / 个人主页”的展示数据

SET NAMES utf8mb4;

-- 1) 定位目标用户
SET @target_name := '我不想上班但想听歌';
SET @target_user_id := (
    SELECT id FROM consumer WHERE username = @target_name LIMIT 1
);

-- 若未找到用户，可先创建一个基础账号（避免脚本直接报错）
INSERT INTO consumer (username, password, sex, phone_num, email, birth, introduction, location, avator, status, create_time, update_time)
SELECT
    @target_name,
    'e10adc3949ba59abbe56e057f20f883e', -- 123456(md5)
    1,
    CONCAT('188', LPAD(FLOOR(RAND() * 100000000), 8, '0')),
    CONCAT('demo_', FLOOR(RAND() * 100000), '@example.com'),
    '1998-08-08 00:00:00',
    '白天通勤打工人，晚上听歌回血。喜欢城市流行、轻电子和夜路感歌单。',
    '上海',
    '/img/consumerPic/tubiao.jpg',
    1,
    NOW(),
    NOW()
WHERE @target_user_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM consumer WHERE username = @target_name);

SET @target_user_id := (
    SELECT id FROM consumer WHERE username = @target_name LIMIT 1
);

-- 2) 丰富用户资料
UPDATE consumer
SET
    introduction = COALESCE(NULLIF(introduction, ''), '白天通勤打工人，晚上听歌回血。喜欢城市流行、轻电子和夜路感歌单。'),
    location = COALESCE(NULLIF(location, ''), '上海'),
    avator = COALESCE(NULLIF(avator, ''), '/img/consumerPic/tubiao.jpg'),
    update_time = NOW()
WHERE id = @target_user_id;

-- 3) 关注关系（我关注别人）
INSERT INTO follow (user_id, follow_user_id, create_time)
SELECT @target_user_id, t.id, NOW()
FROM (
    SELECT c.id
    FROM consumer c
    WHERE c.id <> @target_user_id
    ORDER BY c.id ASC
    LIMIT 8
) t
WHERE @target_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM follow f
      WHERE f.user_id = @target_user_id AND f.follow_user_id = t.id
  );

-- 4) 粉丝关系（别人关注我）
INSERT INTO follow (user_id, follow_user_id, create_time)
SELECT t.id, @target_user_id, NOW()
FROM (
    SELECT c.id
    FROM consumer c
    WHERE c.id <> @target_user_id
    ORDER BY c.id DESC
    LIMIT 10
) t
WHERE @target_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM follow f
      WHERE f.user_id = t.id AND f.follow_user_id = @target_user_id
  );

-- 5) 生成“我的笔记”展示数据（含图片）
-- 图片优先取 song.pic 的真实路径，避免写死路径失效
INSERT INTO post (consumer_id, title, content, cover_url, images, topic, status, like_count, comment_count, create_time, update_time)
SELECT
    @target_user_id,
    x.title,
    x.content,
    x.cover_url,
    x.images,
    x.topic,
    1,
    0,
    0,
    DATE_SUB(NOW(), INTERVAL x.hours_ago HOUR),
    DATE_SUB(NOW(), INTERVAL x.hours_ago HOUR)
FROM (
    SELECT
        '早高峰地铁循环歌单' AS title,
        '地铁上戴上耳机，站着都能满血。今天最上头的是副歌一出来就提神的那首。' AS content,
        COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 0), '/img/consumerPic/tubiao.jpg') AS cover_url,
        CONCAT(
            '["',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 0), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '","',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 1), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '"]'
        ) AS images,
        '日常' AS topic,
        72 AS hours_ago
    UNION ALL
    SELECT
        '午休 20 分钟复活计划',
        '午休不开会的日子，就是我的小型 live 现场。闭眼听一首，下午继续冲。',
        COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 2), '/img/consumerPic/tubiao.jpg'),
        CONCAT(
            '["',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 2), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '"]'
        ),
        '治愈',
        54
    UNION ALL
    SELECT
        '下班路上不要想工作',
        '从公司门口到地铁站这段路，BPM 要稳，心率也要稳。今天这组鼓点很对味。',
        COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 3), '/img/consumerPic/tubiao.jpg'),
        CONCAT(
            '["',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 3), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '","',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 4), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '","',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 5), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '"]'
        ),
        '情绪',
        36
    UNION ALL
    SELECT
        '周五晚风与城市灯',
        '周五的风里有自由感。一个人走路，耳机里是前奏慢慢铺开的那种歌。',
        COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 6), '/img/consumerPic/tubiao.jpg'),
        CONCAT(
            '["',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 6), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '"]'
        ),
        '夜晚',
        18
    UNION ALL
    SELECT
        '今天最想单曲循环',
        '遇到这种副歌就想马上分享：好听，且很适合重复播放。',
        COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 7), '/img/consumerPic/tubiao.jpg'),
        CONCAT(
            '["',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 7), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '","',
            REPLACE(COALESCE((SELECT s.pic FROM song s WHERE s.pic IS NOT NULL AND s.pic <> '' ORDER BY s.id LIMIT 1 OFFSET 8), '/img/consumerPic/tubiao.jpg'), '"', '\\"'),
            '"]'
        ),
        '推荐',
        6
) x
WHERE @target_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM post p
      WHERE p.consumer_id = @target_user_id
        AND p.title = x.title
  );

-- 6) 让目标用户点赞一些他人帖子（用于“我点赞的”）
INSERT INTO post_like (post_id, consumer_id, create_time)
SELECT p.id, @target_user_id, DATE_SUB(NOW(), INTERVAL 2 HOUR)
FROM post p
WHERE @target_user_id IS NOT NULL
  AND p.consumer_id <> @target_user_id
  AND p.status = 1
ORDER BY p.create_time DESC
LIMIT 12
ON DUPLICATE KEY UPDATE create_time = VALUES(create_time);

-- 7) 给目标用户的帖子增加外部点赞（提升热度展示）
INSERT INTO post_like (post_id, consumer_id, create_time)
SELECT tp.id, u.id, DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM (
    SELECT p.id
    FROM post p
    WHERE p.consumer_id = @target_user_id AND p.status = 1
    ORDER BY p.create_time DESC
    LIMIT 5
) tp
JOIN (
    SELECT c.id
    FROM consumer c
    WHERE c.id <> @target_user_id
    ORDER BY c.id ASC
    LIMIT 15
) u
ON 1 = 1
WHERE @target_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM post_like pl
      WHERE pl.post_id = tp.id AND pl.consumer_id = u.id
  );

-- 8) 给目标用户帖子增加评论
INSERT INTO post_comment (post_id, consumer_id, content, create_time)
SELECT
    tp.id,
    u.id,
    CASE MOD(u.id, 5)
        WHEN 0 THEN '这个分享太及时了，今天刚好需要。'
        WHEN 1 THEN '歌单气质很统一，收藏了。'
        WHEN 2 THEN '封面和文案都很有感觉。'
        WHEN 3 THEN '这首我也在循环，握手。'
        ELSE '继续更，等你下一条推荐！'
    END,
    DATE_SUB(NOW(), INTERVAL MOD(u.id, 24) HOUR)
FROM (
    SELECT p.id
    FROM post p
    WHERE p.consumer_id = @target_user_id AND p.status = 1
    ORDER BY p.create_time DESC
    LIMIT 5
) tp
JOIN (
    SELECT c.id
    FROM consumer c
    WHERE c.id <> @target_user_id
    ORDER BY c.id DESC
    LIMIT 10
) u
ON 1 = 1
WHERE @target_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM post_comment pc
      WHERE pc.post_id = tp.id
        AND pc.consumer_id = u.id
        AND pc.content = CASE MOD(u.id, 5)
            WHEN 0 THEN '这个分享太及时了，今天刚好需要。'
            WHEN 1 THEN '歌单气质很统一，收藏了。'
            WHEN 2 THEN '封面和文案都很有感觉。'
            WHEN 3 THEN '这首我也在循环，握手。'
            ELSE '继续更，等你下一条推荐！'
        END
  );

-- 9) 生成播放记录（用于个人中心“历史播放记录”）
INSERT INTO play_record (consumer_id, song_id, play_time, play_seconds, source)
SELECT
    @target_user_id,
    s.id,
    DATE_SUB(NOW(), INTERVAL (@rn := @rn + 1) HOUR),
    120 + MOD(s.id, 140),
    2
FROM (SELECT @rn := 0) r, song s
WHERE @target_user_id IS NOT NULL
ORDER BY s.id DESC
LIMIT 30;

-- 10) 回填帖子点赞/评论计数（确保前端统计准确）
UPDATE post p
SET p.like_count = (
    SELECT COUNT(1) FROM post_like pl WHERE pl.post_id = p.id
)
WHERE p.consumer_id = @target_user_id;

UPDATE post p
SET p.comment_count = (
    SELECT COUNT(1) FROM post_comment pc WHERE pc.post_id = p.id
)
WHERE p.consumer_id = @target_user_id;

-- 11) 输出结果概览
SELECT 'target_user' AS metric, @target_user_id AS val
UNION ALL
SELECT 'my_posts', COUNT(1) FROM post WHERE consumer_id = @target_user_id AND status = 1
UNION ALL
SELECT 'my_followings', COUNT(1) FROM follow WHERE user_id = @target_user_id
UNION ALL
SELECT 'my_followers', COUNT(1) FROM follow WHERE follow_user_id = @target_user_id
UNION ALL
SELECT 'my_liked_posts', COUNT(1) FROM post_like WHERE consumer_id = @target_user_id
UNION ALL
SELECT 'my_play_records', COUNT(1) FROM play_record WHERE consumer_id = @target_user_id;
