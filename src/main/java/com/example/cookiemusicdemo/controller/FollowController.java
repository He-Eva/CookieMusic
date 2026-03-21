package com.example.cookiemusicdemo.controller;

import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.request.FollowRequest;
import com.example.cookiemusicdemo.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class FollowController {

    @Autowired
    private FollowService followService;

    // 关注
    @PostMapping("/follow")
    public R follow(@RequestBody FollowRequest request) {
        return followService.follow(request);
    }

    // 取消关注
    @PostMapping("/follow/delete")
    public R unfollow(@RequestBody FollowRequest request) {
        return followService.unfollow(request);
    }

    // 是否关注
    @PostMapping("/follow/status")
    public R isFollowing(@RequestBody FollowRequest request) {
        return followService.isFollowing(request);
    }

    // 我关注的人
    @GetMapping("/followings")
    public R followingList(@RequestParam Integer userId) {
        return followService.followingList(userId);
    }

    // 关注我的人（粉丝）
    @GetMapping("/followers")
    public R followerList(@RequestParam Integer userId) {
        return followService.followerList(userId);
    }

    // 我关注的用户详情列表
    @GetMapping("/followings/users")
    public R followingUsers(@RequestParam Integer userId) {
        return followService.followingUsers(userId);
    }

    // 粉丝用户详情列表
    @GetMapping("/followers/users")
    public R followerUsers(@RequestParam Integer userId) {
        return followService.followerUsers(userId);
    }
}

