package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.PostCommentMapper;
import com.example.cookiemusicdemo.mapper.PostLikeMapper;
import com.example.cookiemusicdemo.mapper.PostMapper;
import com.example.cookiemusicdemo.model.domain.Post;
import com.example.cookiemusicdemo.model.domain.PostComment;
import com.example.cookiemusicdemo.model.domain.PostLike;
import com.example.cookiemusicdemo.model.request.PostCommentRequest;
import com.example.cookiemusicdemo.model.request.PostLikeRequest;
import com.example.cookiemusicdemo.model.request.PostRequest;
import com.example.cookiemusicdemo.model.vo.PostVO;
import com.example.cookiemusicdemo.service.PostService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Override
    @CacheEvict(value = "post_list", allEntries = true)
    public R addPost(PostRequest postRequest) {
        if (postRequest == null || postRequest.getConsumerId() == null || StringUtils.isBlank(postRequest.getContent())) {
            return R.error("参数错误");
        }
        Post post = new Post();
        BeanUtils.copyProperties(postRequest, post);
        post.setStatus((byte) 1);
        post.setLikeCount(0);
        post.setCommentCount(0);
        if (postMapper.insert(post) > 0) {
            return R.success("发布成功", post.getId());
        }
        return R.error("发布失败");
    }

    @Override
    public R postDetail(Long id) {
        if (id == null) {
            return R.error("参数错误");
        }
        PostVO post = postMapper.selectPostDetail(id);
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            return R.error("内容不存在");
        }
        return R.success("详情", post);
    }

    @Override
    @Cacheable(
            value = "post_list",
            key = "T(String).valueOf(#feed)+':' + T(String).valueOf(#consumerId)+':' + T(String).valueOf(#order)+':' + T(String).valueOf(#topic)+':' + T(String).valueOf(#pageNum)+':' + T(String).valueOf(#pageSize)",
            unless = "#result == null || #result.success == false"
    )
    public R listPost(Integer pageNum, Integer pageSize, String order, String topic, String feed, Integer consumerId) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pn - 1) * ps;

        Map<String, Object> data = new HashMap<>();
        boolean followingFeed = "following".equalsIgnoreCase(feed);
        if (followingFeed) {
            if (consumerId == null) {
                return R.error("请先登录");
            }
            data.put("items", postMapper.selectFollowingPostPage(consumerId, offset, ps, order, topic));
            data.put("total", postMapper.countFollowingPosts(consumerId, topic));
        } else {
            data.put("items", postMapper.selectPostPage(offset, ps, order, topic));
            QueryWrapper<Post> countWrapper = new QueryWrapper<>();
            countWrapper.eq("status", 1);
            if (StringUtils.isNotBlank(topic)) {
                countWrapper.eq("topic", topic);
            }
            data.put("total", postMapper.selectCount(countWrapper));
        }
        data.put("pageNum", pn);
        data.put("pageSize", ps);
        return R.success("列表", data);
    }

    @Transactional
    @Override
    @CacheEvict(value = "post_list", allEntries = true)
    public R likePost(PostLikeRequest request) {
        if (request == null || request.getPostId() == null || request.getConsumerId() == null) {
            return R.error("参数错误");
        }

        long postId = request.getPostId();
        int consumerId = request.getConsumerId();

        // Ensure post exists (and is visible)
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            return R.error("内容不存在");
        }

        QueryWrapper<PostLike> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", postId).eq("consumer_id", consumerId);
        boolean alreadyLiked = postLikeMapper.selectCount(wrapper) > 0;

        Boolean like = request.getLike();
        boolean wantLike = (like == null) ? !alreadyLiked : like;

        if (wantLike) {
            if (alreadyLiked) {
                return R.success("已点赞", true);
            }
            PostLike postLike = new PostLike();
            postLike.setPostId(postId);
            postLike.setConsumerId(consumerId);
            try {
                if (postLikeMapper.insert(postLike) > 0) {
                    postMapper.incrementLikeCount(postId);
                    return R.success("点赞成功", true);
                }
                return R.error("点赞失败");
            } catch (DuplicateKeyException e) {
                // unique constraint (post_id, consumer_id)
                return R.success("已点赞", true);
            }
        } else {
            if (!alreadyLiked) {
                return R.success("未点赞", false);
            }
            if (postLikeMapper.delete(wrapper) > 0) {
                postMapper.decrementLikeCount(postId);
                return R.success("取消点赞", false);
            }
            return R.error("取消点赞失败");
        }
    }

    @Transactional
    @Override
    @CacheEvict(value = "post_list", allEntries = true)
    public R addComment(PostCommentRequest request) {
        if (request == null || request.getPostId() == null || request.getConsumerId() == null || StringUtils.isBlank(request.getContent())) {
            return R.error("参数错误");
        }
        long postId = request.getPostId();
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            return R.error("内容不存在");
        }

        PostComment comment = new PostComment();
        BeanUtils.copyProperties(request, comment);
        if (postCommentMapper.insert(comment) > 0) {
            postMapper.incrementCommentCount(postId);
            return R.success("评论成功", comment.getId());
        }
        return R.error("评论失败");
    }

    @Override
    public R listComment(Long postId, Integer pageNum, Integer pageSize) {
        if (postId == null) {
            return R.error("参数错误");
        }
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pn - 1) * ps;

        Map<String, Object> data = new HashMap<>();
        data.put("items", postCommentMapper.selectCommentPage(postId, offset, ps));

        QueryWrapper<PostComment> countWrapper = new QueryWrapper<>();
        countWrapper.eq("post_id", postId);
        data.put("total", postCommentMapper.selectCount(countWrapper));
        data.put("pageNum", pn);
        data.put("pageSize", ps);

        return R.success("评论列表", data);
    }
}

