package com.example.cookiemusicdemo.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PostVO implements Serializable {
    private Long id;
    private Integer consumerId;
    private String title;
    private String content;
    private String coverUrl;
    private String images;
    private String topic;
    private Integer refSongId;
    private String refSongName;
    private Byte status;
    private Integer likeCount;
    private Integer commentCount;
    private Date createTime;
    private Date updateTime;

    private String authorName;
    private String authorAvatar;
}

