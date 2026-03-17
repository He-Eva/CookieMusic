package com.example.cookiemusicdemo.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "play_record")
@Data
public class PlayRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("consumer_id")
    private Integer consumerId;

    @TableField("song_id")
    private Integer songId;

    @TableField("play_time")
    private Date playTime;

    @TableField("play_seconds")
    private Integer playSeconds;

    private Byte source;
}

