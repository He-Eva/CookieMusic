package com.example.cookiemusicdemo.controller;

import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.request.PostCommentRequest;
import com.example.cookiemusicdemo.model.request.PostLikeRequest;
import com.example.cookiemusicdemo.model.request.PostRequest;
import com.example.cookiemusicdemo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
public class PostController {

    @Autowired
    private PostService postService;

    // 发布笔记
    @PostMapping("/post/add")
    public R addPost(@RequestBody PostRequest postRequest) {
        return postService.addPost(postRequest);
    }

    // 上传帖子图片
    @PostMapping("/post/image/upload")
    public R uploadPostImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return R.error("请选择图片文件");
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null) {
            int idx = originalName.lastIndexOf('.');
            if (idx >= 0) ext = originalName.substring(idx).toLowerCase();
        }
        if (!".jpg".equals(ext) && !".jpeg".equals(ext) && !".png".equals(ext) && !".gif".equals(ext) && !".webp".equals(ext)) {
            return R.error("仅支持 jpg/jpeg/png/gif/webp");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String ret = MinioUploadController.uploadPostImgFile(file, fileName);
        if ("File uploaded successfully!".equals(ret)) {
            return R.success("上传成功", "/user01/post/img/" + fileName);
        }
        return R.error("上传失败");
    }

    // 笔记列表（分页）
    @GetMapping("/post")
    public R listPost(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String feed,
            @RequestParam(required = false) Integer consumerId
    ) {
        return postService.listPost(pageNum, pageSize, order, topic, feed, consumerId);
    }

    // 笔记详情
    @GetMapping("/post/detail")
    public R postDetail(@RequestParam Long id) {
        return postService.postDetail(id);
    }

    // 点赞/取消点赞（like=true/false/null=toggle）
    @PostMapping("/post/like")
    public R likePost(@RequestBody PostLikeRequest request) {
        return postService.likePost(request);
    }

    // 当前用户是否已点赞该帖子
    @PostMapping("/post/like/status")
    public R likeStatus(@RequestBody PostLikeRequest request) {
        return postService.likeStatus(request);
    }

    // 添加评论
    @PostMapping("/post/comment/add")
    public R addComment(@RequestBody PostCommentRequest request) {
        return postService.addComment(request);
    }

    // 评论列表（分页）
    @GetMapping("/post/comment")
    public R listComment(
            @RequestParam Long postId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return postService.listComment(postId, pageNum, pageSize);
    }

    // 我的笔记（分页）
    @GetMapping("/post/user")
    public R listUserPost(
            @RequestParam Integer consumerId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return postService.listUserPost(consumerId, pageNum, pageSize);
    }

    // 我点赞的笔记（分页）
    @GetMapping("/post/liked")
    public R listLikedPost(
            @RequestParam Integer consumerId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return postService.listLikedPost(consumerId, pageNum, pageSize);
    }
}

