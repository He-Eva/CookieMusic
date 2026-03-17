package com.example.cookiemusicdemo.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "follow")
@Data
public class Follow implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("follow_user_id")
    private Integer followUserId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
}

