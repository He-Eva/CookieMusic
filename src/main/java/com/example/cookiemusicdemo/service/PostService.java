package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.domain.Post;
import com.example.cookiemusicdemo.model.request.PostCommentRequest;
import com.example.cookiemusicdemo.model.request.PostLikeRequest;
import com.example.cookiemusicdemo.model.request.PostRequest;

public interface PostService extends IService<Post> {

    R addPost(PostRequest postRequest);

    R postDetail(Long id);

    R listPost(Integer pageNum, Integer pageSize, String order, String topic, String feed, Integer consumerId);

    R likePost(PostLikeRequest request);

    R addComment(PostCommentRequest request);

    R listComment(Long postId, Integer pageNum, Integer pageSize);
}

