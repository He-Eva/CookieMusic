package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.domain.Consumer;
import com.example.cookiemusicdemo.model.request.ConsumerRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

public interface ConsumerService extends IService<Consumer> {

    R addUser(ConsumerRequest registryRequest);

    R updateUserMsg(ConsumerRequest updateRequest);

    R updateUserAvator(MultipartFile avatorFile, int id);

    R updatePassword(ConsumerRequest updatePasswordRequest);

    boolean existUser(String username);

    boolean verityPasswd(String username, String password);

    R deleteUser(Integer id);

    R allUser();

    R userOfId(Integer id);

    R loginStatus(ConsumerRequest loginRequest, HttpSession session);

    R adminUserPage(Integer pageNum, Integer pageSize, String keyword, Integer status);

    R adminUpdateUserStatus(Integer userId, Integer status);

}
