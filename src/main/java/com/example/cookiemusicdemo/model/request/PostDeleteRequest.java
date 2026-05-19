package com.example.cookiemusicdemo.model.request;

import lombok.Data;

@Data
public class PostDeleteRequest {
    private Long postId;
    private Integer consumerId;
}

