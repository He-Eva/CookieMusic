package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.domain.Song;
import com.example.cookiemusicdemo.model.request.SongRequest;
import org.springframework.web.multipart.MultipartFile;

public interface SongService extends IService<Song> {

    R addSong (SongRequest addSongRequest,MultipartFile lrcfile,  MultipartFile mpfile);

    R updateSongMsg(SongRequest updateSongRequest);

    R updateSongUrl(MultipartFile urlFile, int id);

    R updateSongPic(MultipartFile urlFile, int id);

    R deleteSong(Integer id);

    R allSong();

    R songOfSingerId(Integer singerId);

    R songOfId(Integer id);

    R songOfSingerName(String name);

    R updateSongLrc(MultipartFile lrcFile, int id);

    /** 管理端：分页列表（含下架），支持歌名/歌手名关键词与状态筛选 */
    R adminSongPage(Integer pageNum, Integer pageSize, String keyword, Integer status);

    /** 管理端：单条详情（含歌词），不受下架限制 */
    R adminSongDetail(Integer id);

    /** 管理端：上架/下架 status 0/1 */
    R adminUpdateSongStatus(Integer songId, Integer status);

    /** 管理端：更新歌名、简介、歌词（JSON） */
    R adminUpdateSongInfo(SongRequest request);
}
