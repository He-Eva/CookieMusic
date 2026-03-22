-- 歌单上架状态：0=下架（前台不展示），1=上架；旧数据 NULL 视为上架
ALTER TABLE song_list
ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0-下架,1-上架' AFTER introduction;
