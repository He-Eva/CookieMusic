-- CookieMusicDemo schema upgrade
-- Follow relationship: consumer -> consumer
-- Safe to run multiple times.

CREATE TABLE IF NOT EXISTS `follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT 'follower consumer id',
  `follow_user_id` INT NOT NULL COMMENT 'followed consumer id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow_user_follow_user` (`user_id`, `follow_user_id`),
  KEY `idx_follow_user_time` (`user_id`, `create_time`),
  KEY `idx_follow_follow_user_time` (`follow_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

