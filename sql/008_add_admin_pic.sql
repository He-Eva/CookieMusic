-- 管理员头像（可选）：执行后可在 Admin 实体中增加 pic 字段并实现上传接口
-- 若列已存在会报错，忽略即可

ALTER TABLE `admin`
  ADD COLUMN `pic` VARCHAR(255) NULL DEFAULT NULL COMMENT '头像路径，如 /user01/consumer/img/xxx.jpg' AFTER `password`;
