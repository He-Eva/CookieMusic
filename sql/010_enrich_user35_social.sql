SET NAMES utf8mb4;

-- 目标用户：不想上班但想听歌
SET @target_uid := (
  SELECT id FROM consumer WHERE username = '不想上班但想听歌' LIMIT 1
);

-- 1) 我关注别人（10个）
INSERT INTO follow (user_id, follow_user_id, create_time)
SELECT @target_uid, t.id, DATE_SUB(NOW(), INTERVAL (t.rn + 1) DAY)
FROM (
  SELECT c.id, ROW_NUMBER() OVER (ORDER BY c.id DESC) AS rn
  FROM consumer c
  WHERE c.id <> @target_uid
  LIMIT 10
) t
WHERE @target_uid IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM follow f
    WHERE f.user_id = @target_uid AND f.follow_user_id = t.id
  );

-- 2) 别人关注我（12个）
INSERT INTO follow (user_id, follow_user_id, create_time)
SELECT t.id, @target_uid, DATE_SUB(NOW(), INTERVAL (t.rn + 2) DAY)
FROM (
  SELECT c.id, ROW_NUMBER() OVER (ORDER BY c.id ASC) AS rn
  FROM consumer c
  WHERE c.id <> @target_uid
  LIMIT 12
) t
WHERE @target_uid IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM follow f
    WHERE f.user_id = t.id AND f.follow_user_id = @target_uid
  );

-- 3) 我点赞他人帖子（18条）
INSERT INTO post_like (post_id, consumer_id, create_time)
SELECT p.id, @target_uid, DATE_SUB(NOW(), INTERVAL seq.n HOUR)
FROM (
  SELECT id, ROW_NUMBER() OVER (ORDER BY create_time DESC, id DESC) AS n
  FROM post
  WHERE status = 1 AND consumer_id <> @target_uid
  LIMIT 18
) seq
JOIN post p ON p.id = seq.id
WHERE @target_uid IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM post_like pl
    WHERE pl.post_id = p.id AND pl.consumer_id = @target_uid
  );

-- 4) 他人点赞我的帖子（按现有我的帖子尽量补满）
INSERT INTO post_like (post_id, consumer_id, create_time)
SELECT mp.id, u.id, DATE_SUB(NOW(), INTERVAL (u.rn + mp.rn) HOUR)
FROM (
  SELECT p.id, ROW_NUMBER() OVER (ORDER BY p.create_time DESC, p.id DESC) AS rn
  FROM post p
  WHERE p.consumer_id = @target_uid AND p.status = 1
  LIMIT 10
) mp
JOIN (
  SELECT c.id, ROW_NUMBER() OVER (ORDER BY c.id DESC) AS rn
  FROM consumer c
  WHERE c.id <> @target_uid
  LIMIT 20
) u
WHERE @target_uid IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM post_like pl
    WHERE pl.post_id = mp.id AND pl.consumer_id = u.id
  );

-- 5) 他人评论我的帖子（真实用户评论）
INSERT INTO post_comment (post_id, consumer_id, content, create_time)
SELECT mp.id, u.id,
  CASE MOD(u.rn, 6)
    WHEN 0 THEN '这个分享很对味，已加入通勤循环。'
    WHEN 1 THEN '文案和封面都很有感觉。'
    WHEN 2 THEN '这首歌我也在单曲循环。'
    WHEN 3 THEN '蹲一个你的下期推荐。'
    WHEN 4 THEN '收藏了，今晚就听。'
    ELSE '节奏感很舒服，感谢分享。'
  END AS content,
  DATE_SUB(NOW(), INTERVAL (u.rn * 2 + mp.rn) HOUR)
FROM (
  SELECT p.id, ROW_NUMBER() OVER (ORDER BY p.create_time DESC, p.id DESC) AS rn
  FROM post p
  WHERE p.consumer_id = @target_uid AND p.status = 1
  LIMIT 8
) mp
JOIN (
  SELECT c.id, ROW_NUMBER() OVER (ORDER BY c.id ASC) AS rn
  FROM consumer c
  WHERE c.id <> @target_uid
  LIMIT 16
) u
WHERE @target_uid IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM post_comment pc
    WHERE pc.post_id = mp.id AND pc.consumer_id = u.id
  );

-- 6) 个人主页：收藏歌曲（20首）
INSERT INTO collect (user_id, type, song_id, song_list_id, create_time)
SELECT @target_uid, 0, s.id, NULL, DATE_SUB(NOW(), INTERVAL seq.n DAY)
FROM (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id DESC) AS n
  FROM song
  WHERE status = 1
  LIMIT 20
) seq
JOIN song s ON s.id = seq.id
WHERE @target_uid IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM collect c
    WHERE c.user_id = @target_uid AND c.type = 0 AND c.song_id = s.id
  );

-- 7) 个人主页：收藏歌单（8个）
INSERT INTO collect (user_id, type, song_id, song_list_id, create_time)
SELECT @target_uid, 1, NULL, sl.id, DATE_SUB(NOW(), INTERVAL (seq.n + 1) DAY)
FROM (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id DESC) AS n
  FROM song_list
  WHERE status = 1
  LIMIT 8
) seq
JOIN song_list sl ON sl.id = seq.id
WHERE @target_uid IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM collect c
    WHERE c.user_id = @target_uid AND c.type = 1 AND c.song_list_id = sl.id
  );

-- 8) 回填帖子计数（点赞/评论）
UPDATE post p
SET p.like_count = (SELECT COUNT(1) FROM post_like pl WHERE pl.post_id = p.id),
    p.comment_count = (SELECT COUNT(1) FROM post_comment pc WHERE pc.post_id = p.id)
WHERE p.status = 1;

-- 9) 结果汇总
SELECT 'target_uid' AS metric, @target_uid AS val
UNION ALL SELECT 'my_posts', COUNT(*) FROM post WHERE consumer_id=@target_uid AND status=1
UNION ALL SELECT 'my_likes', COUNT(*) FROM post_like WHERE consumer_id=@target_uid
UNION ALL SELECT 'followings', COUNT(*) FROM follow WHERE user_id=@target_uid
UNION ALL SELECT 'followers', COUNT(*) FROM follow WHERE follow_user_id=@target_uid
UNION ALL SELECT 'my_post_comments', COUNT(*) FROM post_comment WHERE post_id IN (SELECT id FROM post WHERE consumer_id=@target_uid AND status=1)
UNION ALL SELECT 'collect_songs', COUNT(*) FROM collect WHERE user_id=@target_uid AND type=0
UNION ALL SELECT 'collect_songlists', COUNT(*) FROM collect WHERE user_id=@target_uid AND type=1;
