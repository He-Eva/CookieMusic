package com.example.cookiemusicdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cookiemusicdemo.model.domain.Comment;
import com.example.cookiemusicdemo.model.vo.AdminCommentVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentMapper extends BaseMapper<Comment> {

    List<AdminCommentVO> selectAdminCommentPage(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("keyword") String keyword
    );

    long countAdminComments(@Param("keyword") String keyword);
}