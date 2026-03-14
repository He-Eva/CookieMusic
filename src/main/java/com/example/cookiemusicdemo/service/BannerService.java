package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.model.domain.Banner;

import java.util.List;


public interface BannerService extends IService<Banner> {

    List<Banner> getAllBanner();

}
