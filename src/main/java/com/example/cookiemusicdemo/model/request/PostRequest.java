package com.example.cookiemusicdemo.model.request;

import lombok.Data;

@Data
public class PostRequest {
    private Integer consumerId;
    private String title;
    private String content;
    private String coverUrl;
    /**
     * Optional: JSON array string of image URLs/paths.
     */
    private String images;
    private String topic;
}

