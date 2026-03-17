package com.example.cookiemusicdemo.model.request;

import lombok.Data;

@Data
public class FollowRequest {
    private Integer userId;
    private Integer followUserId;
}

