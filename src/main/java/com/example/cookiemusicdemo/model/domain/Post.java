package com.example.cookiemusicdemo.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "post")
@Data
public class Post implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("consumer_id")
    private Integer consumerId;

    private String title;

    private String content;

    @TableField("cover_url")
    private String coverUrl;

    /**
     * Optional: JSON array string of image URLs/paths.
     */
    private String images;

    private String topic;

    private Byte status;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}

