package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.domain.Singer;
import com.example.cookiemusicdemo.model.request.SingerRequest;
import org.springframework.web.multipart.MultipartFile;

public interface SingerService extends IService<Singer> {

    R addSinger (SingerRequest addSingerRequest);

    R updateSingerMsg(SingerRequest updateSingerRequest);

    R updateSingerPic(MultipartFile avatorFile, int id);

    R deleteSinger(Integer id);

    R allSinger();

    R singerOfName(String name);

    R singerOfSex(Integer sex);
}
