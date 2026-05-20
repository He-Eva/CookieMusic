SET NAMES utf8mb4;

-- 删除无法播放的歌曲（HTTP 探测失败 / MinIO 无有效音频）
-- 本次目标 id: 63, 114, 117（由 tools/find_unplayable_songs.py 扫描得出，可重跑脚本更新）

DROP TABLE IF EXISTS backup_unplayable_song_deleted;
CREATE TABLE backup_unplayable_song_deleted AS
SELECT so.*, NOW() AS deleted_at
FROM song so
WHERE so.id IN (63, 114, 117);

START TRANSACTION;

DELETE FROM user_support
WHERE comment_id IN (
    SELECT id FROM comment
    WHERE type = 0 AND song_id IN (63, 114, 117)
);

DELETE FROM comment
WHERE type = 0 AND song_id IN (63, 114, 117);

DELETE FROM collect
WHERE type = 0 AND song_id IN (63, 114, 117);

DELETE FROM play_record
WHERE song_id IN (63, 114, 117);

DELETE FROM list_song
WHERE song_id IN (63, 114, 117);

UPDATE post SET ref_song_id = NULL, ref_song_name = NULL
WHERE ref_song_id IN (63, 114, 117);

DELETE FROM song
WHERE id IN (63, 114, 117);

COMMIT;

SELECT COUNT(*) AS remaining FROM song WHERE id IN (63, 114, 117);
