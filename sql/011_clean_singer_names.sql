SET NAMES utf8mb4;

-- 1) 备份（只做一次）
CREATE TABLE IF NOT EXISTS singer_name_backup (
  id INT PRIMARY KEY,
  old_name VARCHAR(255) NOT NULL,
  backup_time DATETIME NOT NULL
);

INSERT INTO singer_name_backup (id, old_name, backup_time)
SELECT s.id, s.name, NOW()
FROM singer s
WHERE NOT EXISTS (
  SELECT 1 FROM singer_name_backup b WHERE b.id = s.id
);

-- 2) 清洗规则
-- - 统一分隔符：中文逗号/顿号/斜杠/竖线/下划线/顿号等 -> 英文逗号
-- - 去掉多余空白
-- - 仅保留主歌手（第一个分段）
UPDATE singer
SET name = TRIM(
  SUBSTRING_INDEX(
    REGEXP_REPLACE(
      REGEXP_REPLACE(name, '[，、/|_&]+', ','),
      '\\s+',
      ' '
    ),
    ',',
    1
  )
)
WHERE name IS NOT NULL
  AND name <> '';

-- 3) 可选细化：少量常见格式修正
UPDATE singer
SET name = 'F.I.R.'
WHERE name IN ('F.I.R', 'FIR');

UPDATE singer
SET name = 'S.H.E'
WHERE name IN ('SHE', 'S.H.E.');

-- 4) 结果核验
SELECT COUNT(*) AS total_singers FROM singer;
SELECT COUNT(*) AS suspicious_after
FROM singer
WHERE name REGEXP '[,，、/|_&]';

SELECT id, name
FROM singer
WHERE id >= 351
ORDER BY id;
