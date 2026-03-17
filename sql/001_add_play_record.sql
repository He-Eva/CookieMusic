-- CookieMusicDemo schema upgrade
-- Adds play history records to support:
-- 1) personal center "history"
-- 2) collaborative filtering recommendations (implicit feedback)
--
-- Safe to run multiple times.

CREATE TABLE IF NOT EXISTS `play_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `consumer_id` INT NOT NULL,
  `song_id` INT NOT NULL,
  `play_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `play_seconds` INT NULL DEFAULT NULL,
  `source` TINYINT NULL DEFAULT NULL COMMENT 'optional: 0=player,1=playlist,2=recommend,etc.',
  PRIMARY KEY (`id`),
  KEY `idx_play_record_consumer_time` (`consumer_id`, `play_time`),
  KEY `idx_play_record_song_time` (`song_id`, `play_time`),
  KEY `idx_play_record_consumer_song` (`consumer_id`, `song_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

