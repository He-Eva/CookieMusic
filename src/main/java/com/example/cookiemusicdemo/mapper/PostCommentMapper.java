package com.example.cookiemusicdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cookiemusicdemo.model.domain.PostComment;
import com.example.cookiemusicdemo.model.vo.PostCommentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PostCommentMapper extends BaseMapper<PostComment> {
    List<PostCommentVO> selectCommentPage(
            @Param("postId") long postId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
}

