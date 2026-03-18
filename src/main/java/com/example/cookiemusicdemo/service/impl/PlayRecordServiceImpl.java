package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.PlayRecordMapper;
import com.example.cookiemusicdemo.model.domain.PlayRecord;
import com.example.cookiemusicdemo.model.request.PlayRecordRequest;
import com.example.cookiemusicdemo.service.PlayRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PlayRecordServiceImpl extends ServiceImpl<PlayRecordMapper, PlayRecord> implements PlayRecordService {

    @Autowired
    private PlayRecordMapper playRecordMapper;

    @Override
    @CacheEvict(value = "recommend", allEntries = true)
    public R addPlayRecord(PlayRecordRequest request) {
        if (request == null || request.getConsumerId() == null || request.getSongId() == null) {
            return R.error("参数错误");
        }
        PlayRecord record = new PlayRecord();
        BeanUtils.copyProperties(request, record);
        if (playRecordMapper.insert(record) > 0) {
            return R.success("记录成功", true);
        }
        return R.error("记录失败");
    }

    @Override
    public R listByUser(Integer consumerId, Integer pageNum, Integer pageSize) {
        if (consumerId == null) return R.error("参数错误");
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pn - 1) * ps;

        QueryWrapper<PlayRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("consumer_id", consumerId).orderByDesc("play_time").last("LIMIT " + offset + "," + ps);

        QueryWrapper<PlayRecord> countWrapper = new QueryWrapper<>();
        countWrapper.eq("consumer_id", consumerId);

        Map<String, Object> data = new HashMap<>();
        data.put("items", playRecordMapper.selectList(wrapper));
        data.put("total", playRecordMapper.selectCount(countWrapper));
        data.put("pageNum", pn);
        data.put("pageSize", ps);
        return R.success("历史记录", data);
    }
}

