SET NAMES utf8mb4;

-- 合并同名重复歌手：保留歌曲数最多的 id（并列取最小 id），将其余 id 的歌曲归并后删除重复歌手。
-- 仅 song.singer_id 外键引用 singer；list_song / play_record / collect / comment 均通过 song_id 关联，无需改动。

-- 1) 合并映射备份表（可重复执行，按 drop_id 去重）
CREATE TABLE IF NOT EXISTS singer_merge_backup (
  drop_id INT PRIMARY KEY,
  keep_id INT NOT NULL,
  singer_name VARCHAR(255) NOT NULL,
  backup_time DATETIME NOT NULL
);

START TRANSACTION;

-- 2) 同名歌手分组
DROP TEMPORARY TABLE IF EXISTS tmp_singer_dup_names;
CREATE TEMPORARY TABLE tmp_singer_dup_names AS
SELECT name
FROM singer
WHERE name IS NOT NULL AND TRIM(name) <> ''
GROUP BY name
HAVING COUNT(*) > 1;

DROP TEMPORARY TABLE IF EXISTS tmp_singer_rank;
CREATE TEMPORARY TABLE tmp_singer_rank AS
SELECT
  s.id,
  s.name,
  (SELECT COUNT(*) FROM song so WHERE so.singer_id = s.id) AS song_cnt
FROM singer s
INNER JOIN tmp_singer_dup_names d ON s.name = d.name;

DROP TEMPORARY TABLE IF EXISTS tmp_singer_keep;
CREATE TEMPORARY TABLE tmp_singer_keep AS
SELECT id AS keep_id, name
FROM (
  SELECT
    id,
    name,
    ROW_NUMBER() OVER (PARTITION BY name ORDER BY song_cnt DESC, id ASC) AS rn
  FROM tmp_singer_rank
) ranked
WHERE rn = 1;

DROP TEMPORARY TABLE IF EXISTS tmp_singer_merge;
CREATE TEMPORARY TABLE tmp_singer_merge AS
SELECT
  r.id AS drop_id,
  k.keep_id,
  r.name AS singer_name
FROM tmp_singer_rank r
INNER JOIN tmp_singer_keep k ON r.name = k.name
WHERE r.id <> k.keep_id;

-- 3) 备份映射
INSERT INTO singer_merge_backup (drop_id, keep_id, singer_name, backup_time)
SELECT m.drop_id, m.keep_id, m.singer_name, NOW()
FROM tmp_singer_merge m
ON DUPLICATE KEY UPDATE
  keep_id = VALUES(keep_id),
  singer_name = VALUES(singer_name),
  backup_time = VALUES(backup_time);

-- 4) 归并歌曲归属
UPDATE song so
INNER JOIN tmp_singer_merge m ON so.singer_id = m.drop_id
SET so.singer_id = m.keep_id;

-- 5) 删除已无歌曲的重复歌手（合并后 drop_id 不应再有歌曲）
DELETE s
FROM singer s
INNER JOIN tmp_singer_merge m ON s.id = m.drop_id
WHERE NOT EXISTS (SELECT 1 FROM song so WHERE so.singer_id = s.id);

COMMIT;

-- 6) 核验
SELECT COUNT(*) AS duplicate_name_groups_remaining
FROM (
  SELECT name
  FROM singer
  WHERE name IS NOT NULL AND TRIM(name) <> ''
  GROUP BY name
  HAVING COUNT(*) > 1
) t;

SELECT COUNT(*) AS orphan_songs
FROM song so
LEFT JOIN singer s ON so.singer_id = s.id
WHERE s.id IS NULL;

SELECT drop_id, keep_id, singer_name, backup_time
FROM singer_merge_backup
ORDER BY singer_name, drop_id;
