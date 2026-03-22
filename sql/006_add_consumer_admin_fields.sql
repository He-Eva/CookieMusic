-- 用户表扩展：管理端用户列表 / 禁用 / 看板统计依赖这些字段
-- 若列已存在会跳过，可重复执行（需 MySQL 5.7+）

SET @db := DATABASE();

-- status：0=禁用，1=正常
SELECT COUNT(*) INTO @c FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'consumer' AND COLUMN_NAME = 'status';
SET @sql := IF(@c = 0,
  'ALTER TABLE `consumer` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''0=禁用,1=正常'' AFTER `avator`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- create_time
SELECT COUNT(*) INTO @c FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'consumer' AND COLUMN_NAME = 'create_time';
SET @sql := IF(@c = 0,
  'ALTER TABLE `consumer` ADD COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- update_time
SELECT COUNT(*) INTO @c FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'consumer' AND COLUMN_NAME = 'update_time';
SET @sql := IF(@c = 0,
  'ALTER TABLE `consumer` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
