package com.example.cookiemusicdemo.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PostCommentVO implements Serializable {
    private Long id;
    private Long postId;
    private Integer consumerId;
    private String content;
    private Date createTime;

    private String userName;
    private String userAvatar;
}

