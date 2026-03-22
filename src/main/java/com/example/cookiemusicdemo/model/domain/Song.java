package com.example.cookiemusicdemo.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

@TableName(value = "song")
@Data
public class Song {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer singerId;

    private String name;

    private String introduction;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    private String pic;

    private String lyric;

    private String url;

    /**
     * 上架状态：0-下架，1-上架（前台仅展示上架；NULL 视为上架兼容旧数据）
     */
    private Byte status;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
