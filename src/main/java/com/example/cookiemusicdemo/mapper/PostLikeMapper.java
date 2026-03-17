package com.example.cookiemusicdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cookiemusicdemo.model.domain.PostLike;
import org.apache.ibatis.annotations.Param;

public interface PostLikeMapper extends BaseMapper<PostLike> {
    int deleteByPostAndConsumer(@Param("postId") long postId, @Param("consumerId") int consumerId);
}

