package com.example.cookiemusicdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cookiemusicdemo.model.domain.Follow;
import org.apache.ibatis.annotations.Param;

import java.util.Map;
import java.util.List;

public interface FollowMapper extends BaseMapper<Follow> {
    List<Integer> selectFollowingIds(@Param("userId") int userId);
    List<Integer> selectFollowerIds(@Param("userId") int userId);
    List<Map<String, Object>> selectFollowingUsers(@Param("userId") int userId);
    List<Map<String, Object>> selectFollowerUsers(@Param("userId") int userId);
}

