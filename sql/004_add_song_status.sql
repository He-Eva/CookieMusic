-- 歌曲上架状态：0=下架（前台不展示、不可播放），1=上架；旧数据 NULL 视为上架
ALTER TABLE song
ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0-下架,1-上架' AFTER url;
