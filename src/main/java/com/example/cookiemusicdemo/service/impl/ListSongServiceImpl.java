package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.ListSongMapper;
import com.example.cookiemusicdemo.model.domain.ListSong;
import com.example.cookiemusicdemo.model.request.ListSongRequest;
import com.example.cookiemusicdemo.service.ListSongService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListSongServiceImpl extends ServiceImpl<ListSongMapper, ListSong> implements ListSongService {

    @Autowired
    private ListSongMapper listSongMapper;

    @Override
    public List<ListSong> allListSong() {
        return listSongMapper.selectList(null);
    }

    @Override
    public R updateListSongMsg(ListSongRequest updateListSongRequest) {
        ListSong listSong = new ListSong();
        BeanUtils.copyProperties(updateListSongRequest, listSong);
        if (listSongMapper.updateById(listSong) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    @Override
    public R deleteListSong(Integer songId) {
        QueryWrapper<ListSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_id",songId);
        if (listSongMapper.delete(queryWrapper) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    @Override
    public R addListSong(ListSongRequest addListSongRequest) {
        ListSong listSong = new ListSong();
        BeanUtils.copyProperties(addListSongRequest, listSong);
        if (listSongMapper.insert(listSong) > 0) {
            return R.success("添加成功");
        } else {
            return R.error("添加失败");
        }
    }

    @Override
    public R listSongOfSongId(Integer songListId) {
        QueryWrapper<ListSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id",songListId);
        return R.success("查询成功", listSongMapper.selectList(queryWrapper));
    }

    @Override
    public R adminAddSongToList(Integer songListId, Integer songId) {
        if (songListId == null || songId == null) {
            return R.error("参数错误");
        }
        QueryWrapper<ListSong> exists = new QueryWrapper<>();
        exists.eq("song_list_id", songListId).eq("song_id", songId);
        if (listSongMapper.selectCount(exists) > 0) {
            return R.warning("该歌曲已在歌单中");
        }
        ListSong ls = new ListSong();
        ls.setSongListId(songListId);
        ls.setSongId(songId);
        try {
            if (listSongMapper.insert(ls) > 0) {
                return R.success("添加成功");
            }
            return R.error("添加失败");
        } catch (DuplicateKeyException e) {
            return R.warning("该歌曲已在歌单中");
        }
    }

    @Override
    public R adminRemoveSongFromList(Integer songListId, Integer songId) {
        if (songListId == null || songId == null) {
            return R.error("参数错误");
        }
        QueryWrapper<ListSong> w = new QueryWrapper<>();
        w.eq("song_list_id", songListId).eq("song_id", songId);
        if (listSongMapper.delete(w) > 0) {
            return R.success("已移除");
        }
        return R.error("移除失败或记录不存在");
    }

}
