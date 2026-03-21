package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.domain.Admin;
import com.example.cookiemusicdemo.model.request.AdminRequest;

import javax.servlet.http.HttpSession;

public interface AdminService extends IService<Admin> {

    R verityPasswd(AdminRequest adminRequest, HttpSession session);

    R logout(HttpSession session);

    R updatePassword(AdminRequest adminRequest, HttpSession session);
}
