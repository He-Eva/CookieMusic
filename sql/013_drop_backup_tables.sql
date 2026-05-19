-- 删除开发与数据清洗时产生的备份表（应用代码未引用）。
-- 执行前请自行整库备份；数据库名需与配置一致（默认 tp_music）。
-- 来源：012/011 脚本及 Navicat 手工备份表命名。

USE tp_music;

DROP TABLE IF EXISTS backup_collect_deleted;
DROP TABLE IF EXISTS backup_comment_deleted;
DROP TABLE IF EXISTS backup_list_song_deleted;
DROP TABLE IF EXISTS backup_play_record_deleted;
DROP TABLE IF EXISTS backup_singer_deleted;
DROP TABLE IF EXISTS backup_song_deleted;
DROP TABLE IF EXISTS backup_user_support_deleted;

DROP TABLE IF EXISTS backup_comment_songlist_content_20260417;
DROP TABLE IF EXISTS backup_comment_songlist_userid_20260417;
DROP TABLE IF EXISTS backup_comment_songlist_userid_20260417_v2;

DROP TABLE IF EXISTS singer_name_backup;
