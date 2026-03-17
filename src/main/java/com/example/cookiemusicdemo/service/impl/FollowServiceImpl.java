package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.FollowMapper;
import com.example.cookiemusicdemo.model.domain.Follow;
import com.example.cookiemusicdemo.model.request.FollowRequest;
import com.example.cookiemusicdemo.service.FollowService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    @Override
    @CacheEvict(value = "post_list", allEntries = true)
    public R follow(FollowRequest request) {
        if (request == null || request.getUserId() == null || request.getFollowUserId() == null) {
            return R.error("参数错误");
        }
        if (request.getUserId().equals(request.getFollowUserId())) {
            return R.error("不能关注自己");
        }
        Follow follow = new Follow();
        BeanUtils.copyProperties(request, follow);
        try {
            if (followMapper.insert(follow) > 0) {
                return R.success("关注成功", true);
            }
            return R.error("关注失败");
        } catch (DuplicateKeyException e) {
            return R.success("已关注", true);
        }
    }

    @Override
    @CacheEvict(value = "post_list", allEntries = true)
    public R unfollow(FollowRequest request) {
        if (request == null || request.getUserId() == null || request.getFollowUserId() == null) {
            return R.error("参数错误");
        }
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", request.getUserId()).eq("follow_user_id", request.getFollowUserId());
        if (followMapper.delete(wrapper) > 0) {
            return R.success("取消关注", false);
        }
        return R.success("未关注", false);
    }

    @Override
    public R isFollowing(FollowRequest request) {
        if (request == null || request.getUserId() == null || request.getFollowUserId() == null) {
            return R.error("参数错误");
        }
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", request.getUserId()).eq("follow_user_id", request.getFollowUserId());
        return R.success("关注状态", followMapper.selectCount(wrapper) > 0);
    }

    @Override
    public R followingList(Integer userId) {
        if (userId == null) {
            return R.error("参数错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("ids", followMapper.selectFollowingIds(userId));
        return R.success("关注列表", data);
    }

    @Override
    public R followerList(Integer userId) {
        if (userId == null) {
            return R.error("参数错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("ids", followMapper.selectFollowerIds(userId));
        return R.success("粉丝列表", data);
    }
}

