package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.CollectMapper;
import com.example.cookiemusicdemo.model.domain.Collect;
import com.example.cookiemusicdemo.model.request.CollectRequest;
import com.example.cookiemusicdemo.service.CollectService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CollectServiceImpl extends ServiceImpl<CollectMapper, Collect> implements CollectService {
    @Autowired
    private CollectMapper collectMapper;

    @Override
    public R addCollection(CollectRequest addCollectRequest) {
        //用type来判断收藏的是歌还是歌单
        if (addCollectRequest == null || addCollectRequest.getUserId() == null) {
            return R.error("参数错误");
        }
        Byte type = addCollectRequest.getType();
        if (type == null) type = 0;
        if (type == 1) {
            if (addCollectRequest.getSongListId() == null) return R.error("参数错误");
        } else {
            if (addCollectRequest.getSongId() == null) return R.error("参数错误");
        }

        // 已收藏则不重复插入
        QueryWrapper<Collect> exists = new QueryWrapper<>();
        exists.eq("user_id", addCollectRequest.getUserId());
        exists.eq("type", type);
        if (type == 1) {
            exists.eq("song_list_id", addCollectRequest.getSongListId());
        } else {
            exists.eq("song_id", addCollectRequest.getSongId());
        }
        if (collectMapper.selectCount(exists) > 0) {
            return R.success("已收藏", true);
        }

        Collect collect = new Collect();
        BeanUtils.copyProperties(addCollectRequest, collect);
        collect.setType(type);
        if (collect.getCreateTime() == null) {
            collect.setCreateTime(new Date());
        }
        if (collectMapper.insert(collect) > 0) {
            return R.success("收藏成功", true);
        } else {
            return R.error("收藏失败");
        }
    }

    @Override
    public R existSongId(CollectRequest isCollectRequest) {
        QueryWrapper<Collect> queryWrapper = new QueryWrapper();
        if (isCollectRequest == null || isCollectRequest.getUserId() == null) {
            return R.error("参数错误");
        }
        queryWrapper.eq("user_id", isCollectRequest.getUserId());
        Byte type = isCollectRequest.getType();
        if (type != null && type == 1) {
            queryWrapper.eq("type", 1);
            queryWrapper.eq("song_list_id", isCollectRequest.getSongListId());
        } else {
            queryWrapper.eq("type", 0);
            queryWrapper.eq("song_id", isCollectRequest.getSongId());
        }
        if (collectMapper.selectCount(queryWrapper) > 0) {
            return R.success("已收藏", true);
        } else {
            return R.success("未收藏", false);
        }
    }

    @Override
    public R deleteCollect(Integer userId, Integer songId, Integer songListId, Byte type) {
        if (userId == null) return R.error("参数错误");
        QueryWrapper<Collect> queryWrapper = new QueryWrapper();
        queryWrapper.eq("user_id", userId);
        if (type != null && type == 1) {
            if (songListId == null) return R.error("参数错误");
            queryWrapper.eq("type", 1);
            queryWrapper.eq("song_list_id", songListId);
        } else {
            if (songId == null) return R.error("参数错误");
            queryWrapper.eq("type", 0);
            queryWrapper.eq("song_id", songId);
        }
        if (collectMapper.delete(queryWrapper) > 0) {
            return R.success("取消收藏", false);
        } else {
            return R.error("取消收藏失败");
        }
    }

    @Override
    public R collectionOfUser(Integer userId) {
        QueryWrapper<Collect> queryWrapper = new QueryWrapper();
        queryWrapper.eq("user_id",userId);
        return R.success("用户收藏", collectMapper.selectList(queryWrapper));
    }
}
