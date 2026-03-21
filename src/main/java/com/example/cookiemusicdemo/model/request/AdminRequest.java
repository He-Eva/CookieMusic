package com.example.cookiemusicdemo.model.request;

import lombok.Data;


@Data
public class AdminRequest {
    private Integer id;

    private String username;

    private String oldPassword;

    private String password;
}
