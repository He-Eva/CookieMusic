package com.example.cookiemusicdemo.model.request;

import lombok.Data;

@Data
public class PostCommentRequest {
    private Long postId;
    private Integer consumerId;
    private String content;
}

