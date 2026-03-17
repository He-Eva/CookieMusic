package com.example.cookiemusicdemo.model.request;

import lombok.Data;

@Data
public class PostLikeRequest {
    private Long postId;
    private Integer consumerId;
    /**
     * true=like, false=unlike, null=toggle
     */
    private Boolean like;
}

