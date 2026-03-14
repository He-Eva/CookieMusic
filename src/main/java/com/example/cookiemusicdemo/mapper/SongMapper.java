package com.example.cookiemusicdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cookiemusicdemo.model.domain.Song;
import org.springframework.stereotype.Repository;

@Repository
public interface SongMapper extends BaseMapper<Song> {

}
