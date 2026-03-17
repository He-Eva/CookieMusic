package com.example.cookiemusicdemo.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "post_comment")
@Data
public class PostComment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("consumer_id")
    private Integer consumerId;

    private String content;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
}

