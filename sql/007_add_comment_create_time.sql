-- 评论表：管理端列表按时间排序、实体 FieldFill 依赖 create_time
-- 若列已存在会跳过，可重复执行

SET @db := DATABASE();

SELECT COUNT(*) INTO @c FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'create_time';
SET @sql := IF(@c = 0,
  'ALTER TABLE `comment` ADD COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
