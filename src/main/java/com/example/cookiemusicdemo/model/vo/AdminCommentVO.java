package com.example.cookiemusicdemo.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AdminCommentVO {
    private Integer id;
    private Integer userId;
    private String username;
    private Integer songId;
    private Integer songListId;
    private String content;
    private Date createTime;
    private Byte type;
    private Integer up;
}

