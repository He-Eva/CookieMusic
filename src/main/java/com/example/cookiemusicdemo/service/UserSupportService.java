package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.domain.UserSupport;
import com.example.cookiemusicdemo.model.request.UserSupportRequest;

public interface UserSupportService extends IService<UserSupport> {

    R isUserSupportComment(UserSupportRequest userSupportRequest);

    R insertCommentSupport(UserSupportRequest userSupportRequest);

    R deleteCommentSupport(UserSupportRequest userSupportRequest);
}
