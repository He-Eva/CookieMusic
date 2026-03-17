package com.example.cookiemusicdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cookiemusicdemo.model.domain.Post;
import com.example.cookiemusicdemo.model.vo.PostVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PostMapper extends BaseMapper<Post> {

    List<PostVO> selectPostPage(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("order") String order,
            @Param("topic") String topic
    );

    List<PostVO> selectFollowingPostPage(
            @Param("userId") int userId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("order") String order,
            @Param("topic") String topic
    );

    long countFollowingPosts(@Param("userId") int userId, @Param("topic") String topic);

    PostVO selectPostDetail(@Param("id") long id);

    int incrementLikeCount(@Param("postId") long postId);

    int decrementLikeCount(@Param("postId") long postId);

    int incrementCommentCount(@Param("postId") long postId);
}

