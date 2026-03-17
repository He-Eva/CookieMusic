package com.example.cookiemusicdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.domain.PlayRecord;
import com.example.cookiemusicdemo.model.request.PlayRecordRequest;

public interface PlayRecordService extends IService<PlayRecord> {
    R addPlayRecord(PlayRecordRequest request);
    R listByUser(Integer consumerId, Integer pageNum, Integer pageSize);
}

