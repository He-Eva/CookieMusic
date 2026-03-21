package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.CommentMapper;
import com.example.cookiemusicdemo.model.domain.Comment;
import com.example.cookiemusicdemo.model.request.CommentRequest;
import com.example.cookiemusicdemo.service.CommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    @Autowired
    private CommentMapper commentMapper;

    @Override
    public R addComment(CommentRequest addCommentRequest) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(addCommentRequest, comment);
        comment.setType(addCommentRequest.getNowType());
        if (commentMapper.insert(comment) > 0) {
            return R.success("评论成功");
        } else {
            return R.error("评论失败");
        }
    }

    @Override
    public R updateCommentMsg(CommentRequest addCommentRequest) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(addCommentRequest, comment);
        if (commentMapper.updateById(comment) > 0) {
            return R.success("点赞成功");
        } else {
            return R.error("点赞失败");
        }
    }

    //    删除评论
    @Override
    public R deleteComment(Integer id) {
        if (commentMapper.deleteById(id) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    @Override
    public R commentOfSongId(Integer songId) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_id",songId);
        return R.success(null, commentMapper.selectList(queryWrapper));
    }

    @Override
    public R commentOfSongListId(Integer songListId) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id",songListId);
        return R.success(null, commentMapper.selectList(queryWrapper));
    }

    @Override
    public R adminCommentPage(Integer pageNum, Integer pageSize, String keyword) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pn - 1) * ps;
        Map<String, Object> data = new HashMap<>();
        data.put("items", commentMapper.selectAdminCommentPage(offset, ps, keyword));
        data.put("total", commentMapper.countAdminComments(keyword));
        data.put("pageNum", pn);
        data.put("pageSize", ps);
        return R.success("管理员评论列表", data);
    }

    @Override
    public R adminDeleteComment(Integer id) {
        return deleteComment(id);
    }
}
