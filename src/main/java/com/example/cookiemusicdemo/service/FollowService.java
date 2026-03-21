package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.domain.Follow;
import com.example.cookiemusicdemo.model.request.FollowRequest;

public interface FollowService extends IService<Follow> {
    R follow(FollowRequest request);
    R unfollow(FollowRequest request);
    R isFollowing(FollowRequest request);
    R followingList(Integer userId);
    R followerList(Integer userId);
    R followingUsers(Integer userId);
    R followerUsers(Integer userId);
}

