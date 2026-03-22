package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.AdminMapper;
import com.example.cookiemusicdemo.model.domain.Admin;
import com.example.cookiemusicdemo.model.request.AdminRequest;
import com.example.cookiemusicdemo.service.AdminService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public R verityPasswd(AdminRequest adminRequest, HttpSession session) {
        if (adminRequest == null || StringUtils.isBlank(adminRequest.getName()) || StringUtils.isBlank(adminRequest.getPassword())) {
            return R.error("用户名或密码不能为空");
        }
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", adminRequest.getName());
        queryWrapper.eq("password", adminRequest.getPassword());
        if (adminMapper.selectCount(queryWrapper) > 0) {
            session.setAttribute("name", adminRequest.getName());
            return R.success("登录成功");
        } else {
            return R.error("用户名或密码错误");
        }
    }

    @Override
    public R logout(HttpSession session) {
        if (session != null) {
            session.removeAttribute("name");
            session.invalidate();
        }
        return R.success("退出成功");
    }

    @Override
    public R updatePassword(AdminRequest adminRequest, HttpSession session) {
        String loginName = session == null ? null : (String) session.getAttribute("name");
        if (StringUtils.isBlank(loginName)) {
            return R.error("管理员未登录或登录已过期");
        }
        if (adminRequest == null || StringUtils.isBlank(adminRequest.getOldPassword()) || StringUtils.isBlank(adminRequest.getPassword())) {
            return R.error("参数错误");
        }

        QueryWrapper<Admin> query = new QueryWrapper<>();
        query.eq("name", loginName);
        Admin dbAdmin = adminMapper.selectOne(query);
        if (dbAdmin == null) {
            return R.error("管理员不存在");
        }
        if (!adminRequest.getOldPassword().equals(dbAdmin.getPassword())) {
            return R.error("旧密码错误");
        }
        if (adminRequest.getOldPassword().equals(adminRequest.getPassword())) {
            return R.warning("新密码不能与旧密码相同");
        }

        Admin patch = new Admin();
        patch.setId(dbAdmin.getId());
        patch.setPassword(adminRequest.getPassword());
        if (adminMapper.updateById(patch) > 0) {
            return R.success("管理员密码修改成功");
        }
        return R.error("管理员密码修改失败");
    }
}
