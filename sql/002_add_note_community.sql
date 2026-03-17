-- CookieMusicDemo schema upgrade
-- Note community (posts) minimal schema:
-- - post: user-generated notes/dynamics
-- - post_comment: comments under a post
-- - post_like: like records (also prevents duplicate likes)
--
-- Safe to run multiple times.

CREATE TABLE IF NOT EXISTS `post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `consumer_id` INT NOT NULL,
  `title` VARCHAR(100) NULL DEFAULT NULL,
  `content` TEXT NOT NULL,
  `cover_url` VARCHAR(255) NULL DEFAULT NULL COMMENT 'optional cover image URL/path',
  `images` TEXT NULL DEFAULT NULL COMMENT 'optional: JSON array string of image URLs/paths',
  `topic` VARCHAR(50) NULL DEFAULT NULL COMMENT 'optional: tag/topic',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=normal,0=hidden,2=deleted',
  `like_count` INT NOT NULL DEFAULT 0,
  `comment_count` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_consumer_time` (`consumer_id`, `create_time`),
  KEY `idx_post_time` (`create_time`),
  KEY `idx_post_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `post_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `consumer_id` INT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_comment_post_time` (`post_id`, `create_time`),
  KEY `idx_post_comment_consumer_time` (`consumer_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `post_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `consumer_id` INT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_like_post_consumer` (`post_id`, `consumer_id`),
  KEY `idx_post_like_consumer_time` (`consumer_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

