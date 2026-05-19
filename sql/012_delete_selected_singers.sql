SET NAMES utf8mb4;

-- 目标歌手 ID
-- 351:183Club, 352:5566, 353:Atlus, 359:M3, 364:The S#Arp, 365:ai.mini

-- 1) 备份表（本次删除快照；每次重建）
DROP TABLE IF EXISTS backup_singer_deleted;
CREATE TABLE backup_singer_deleted AS
SELECT s.*, CAST(NULL AS DATETIME) AS deleted_at
FROM singer s
WHERE 1 = 0;

DROP TABLE IF EXISTS backup_song_deleted;
CREATE TABLE backup_song_deleted AS
SELECT so.*, CAST(NULL AS DATETIME) AS deleted_at
FROM song so
WHERE 1 = 0;

DROP TABLE IF EXISTS backup_list_song_deleted;
CREATE TABLE backup_list_song_deleted AS
SELECT ls.*, CAST(NULL AS DATETIME) AS deleted_at
FROM list_song ls
WHERE 1 = 0;

DROP TABLE IF EXISTS backup_play_record_deleted;
CREATE TABLE backup_play_record_deleted AS
SELECT pr.*, CAST(NULL AS DATETIME) AS deleted_at
FROM play_record pr
WHERE 1 = 0;

DROP TABLE IF EXISTS backup_collect_deleted;
CREATE TABLE backup_collect_deleted AS
SELECT c.*, CAST(NULL AS DATETIME) AS deleted_at
FROM collect c
WHERE 1 = 0;

DROP TABLE IF EXISTS backup_comment_deleted;
CREATE TABLE backup_comment_deleted AS
SELECT c.*, CAST(NULL AS DATETIME) AS deleted_at
FROM comment c
WHERE 1 = 0;

DROP TABLE IF EXISTS backup_user_support_deleted;
CREATE TABLE backup_user_support_deleted AS
SELECT us.*, CAST(NULL AS DATETIME) AS deleted_at
FROM user_support us
WHERE 1 = 0;

START TRANSACTION;

-- 2) 备份目标歌手与歌曲
INSERT INTO backup_singer_deleted
SELECT s.*, NOW() AS deleted_at
FROM singer s
WHERE s.id IN (351,352,353,359,364,365)
  AND NOT EXISTS (SELECT 1 FROM backup_singer_deleted b WHERE b.id = s.id);

INSERT INTO backup_song_deleted
SELECT so.*, NOW() AS deleted_at
FROM song so
WHERE so.singer_id IN (351,352,353,359,364,365)
  AND NOT EXISTS (SELECT 1 FROM backup_song_deleted b WHERE b.id = so.id);

-- 3) 备份并清理歌曲关联数据
INSERT INTO backup_list_song_deleted
SELECT ls.*, NOW() AS deleted_at
FROM list_song ls
WHERE ls.song_id IN (SELECT id FROM song WHERE singer_id IN (351,352,353,359,364,365))
  AND NOT EXISTS (SELECT 1 FROM backup_list_song_deleted b WHERE b.id = ls.id);

INSERT INTO backup_play_record_deleted
SELECT pr.*, NOW() AS deleted_at
FROM play_record pr
WHERE pr.song_id IN (SELECT id FROM song WHERE singer_id IN (351,352,353,359,364,365))
  AND NOT EXISTS (SELECT 1 FROM backup_play_record_deleted b WHERE b.id = pr.id);

INSERT INTO backup_collect_deleted
SELECT c.*, NOW() AS deleted_at
FROM collect c
WHERE c.type = 0
  AND c.song_id IN (SELECT id FROM song WHERE singer_id IN (351,352,353,359,364,365))
  AND NOT EXISTS (SELECT 1 FROM backup_collect_deleted b WHERE b.id = c.id);

INSERT INTO backup_comment_deleted
SELECT c.*, NOW() AS deleted_at
FROM comment c
WHERE c.type = 0
  AND c.song_id IN (SELECT id FROM song WHERE singer_id IN (351,352,353,359,364,365))
  AND NOT EXISTS (SELECT 1 FROM backup_comment_deleted b WHERE b.id = c.id);

INSERT INTO backup_user_support_deleted
SELECT us.*, NOW() AS deleted_at
FROM user_support us
WHERE us.comment_id IN (
    SELECT c.id FROM comment c
    WHERE c.type = 0
      AND c.song_id IN (SELECT id FROM song WHERE singer_id IN (351,352,353,359,364,365))
)
  AND NOT EXISTS (SELECT 1 FROM backup_user_support_deleted b WHERE b.id = us.id);

-- 4) 删除关联数据（先子表后主表）
DELETE FROM user_support
WHERE comment_id IN (
    SELECT id FROM backup_comment_deleted
    WHERE song_id IN (SELECT id FROM backup_song_deleted)
);

DELETE FROM comment
WHERE type = 0
  AND song_id IN (SELECT id FROM backup_song_deleted);

DELETE FROM collect
WHERE type = 0
  AND song_id IN (SELECT id FROM backup_song_deleted);

DELETE FROM play_record
WHERE song_id IN (SELECT id FROM backup_song_deleted);

DELETE FROM list_song
WHERE song_id IN (SELECT id FROM backup_song_deleted);

DELETE FROM song
WHERE id IN (SELECT id FROM backup_song_deleted);

DELETE FROM singer
WHERE id IN (351,352,353,359,364,365);

COMMIT;

-- 5) 结果核验
SELECT COUNT(*) AS remaining_target_singers
FROM singer
WHERE id IN (351,352,353,359,364,365);

SELECT COUNT(*) AS remaining_target_songs
FROM song
WHERE singer_id IN (351,352,353,359,364,365);
