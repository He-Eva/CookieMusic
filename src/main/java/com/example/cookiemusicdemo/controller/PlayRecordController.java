package com.example.cookiemusicdemo.controller;

import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.request.PlayRecordRequest;
import com.example.cookiemusicdemo.service.PlayRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlayRecordController {

    @Autowired
    private PlayRecordService playRecordService;

    // 播放记录上报（写库）
    @PostMapping("/playRecord/add")
    public R add(@RequestBody PlayRecordRequest request) {
        return playRecordService.addPlayRecord(request);
    }

    // 查询用户历史播放记录（分页）
    @GetMapping("/playRecord/user")
    public R listByUser(
            @RequestParam Integer consumerId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return playRecordService.listByUser(consumerId, pageNum, pageSize);
    }
}

