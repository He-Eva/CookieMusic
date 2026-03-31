ALTER TABLE post
  ADD COLUMN ref_song_id INT NULL COMMENT '关联歌曲ID',
  ADD COLUMN ref_song_name VARCHAR(255) NULL COMMENT '关联歌曲名';

