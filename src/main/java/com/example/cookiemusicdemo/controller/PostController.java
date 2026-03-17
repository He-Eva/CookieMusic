package com.example.cookiemusicdemo.controller;

import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.request.PostCommentRequest;
import com.example.cookiemusicdemo.model.request.PostLikeRequest;
import com.example.cookiemusicdemo.model.request.PostRequest;
import com.example.cookiemusicdemo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PostController {

    @Autowired
    private PostService postService;

    // 发布笔记
    @PostMapping("/post/add")
    public R addPost(@RequestBody PostRequest postRequest) {
        return postService.addPost(postRequest);
    }

    // 笔记列表（分页）
    @GetMapping("/post")
    public R listPost(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String feed,
            @RequestParam(required = false) Integer consumerId
    ) {
        return postService.listPost(pageNum, pageSize, order, topic, feed, consumerId);
    }

    // 笔记详情
    @GetMapping("/post/detail")
    public R postDetail(@RequestParam Long id) {
        return postService.postDetail(id);
    }

    // 点赞/取消点赞（like=true/false/null=toggle）
    @PostMapping("/post/like")
    public R likePost(@RequestBody PostLikeRequest request) {
        return postService.likePost(request);
    }

    // 添加评论
    @PostMapping("/post/comment/add")
    public R addComment(@RequestBody PostCommentRequest request) {
        return postService.addComment(request);
    }

    // 评论列表（分页）
    @GetMapping("/post/comment")
    public R listComment(
            @RequestParam Long postId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return postService.listComment(postId, pageNum, pageSize);
    }
}

