package com.example.cookiemusicdemo.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 管理员请求体。登录时前端传字段名 {@code username}，与表字段 {@code name} 对应。
 */
@Data
public class AdminRequest {
    private Integer id;

    /** 登录名，与数据库 admin.name 一致；反序列化兼容 JSON 的 username */
    @JsonProperty("username")
    @JsonAlias({"name", "username"})
    private String name;

    private String oldPassword;

    private String password;
}
