package com.example.cookiemusicdemo.controller;

import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.request.AdminPostAuditRequest;
import com.example.cookiemusicdemo.model.request.AdminRequest;
import com.example.cookiemusicdemo.service.AdminService;
import com.example.cookiemusicdemo.service.CommentService;
import com.example.cookiemusicdemo.service.ConsumerService;
import com.example.cookiemusicdemo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * 后台管理的相关事宜
 */
@RestController
public class AdminController {
    @Autowired
    private AdminService adminService;
    @Autowired
    private PostService postService;
    @Autowired
    private ConsumerService consumerService;
    @Autowired
    private CommentService commentService;

    // 判断是否登录成功
    @PostMapping("/admin/login/status")
    public R loginStatus(@RequestBody AdminRequest adminRequest, HttpSession session) {
        return adminService.verityPasswd(adminRequest, session);
    }

    // 管理员退出
    @GetMapping("/admin/logout")
    public R logout(HttpSession session) {
        return adminService.logout(session);
    }

    // 管理员改密（依赖当前已登录 session）
    @PostMapping("/admin/password/update")
    public R updatePassword(@RequestBody AdminRequest adminRequest, HttpSession session) {
        return adminService.updatePassword(adminRequest, session);
    }

    // 管理员帖子列表（可按状态筛选：0待审/1通过/2驳回/3下架）
    @GetMapping("/admin/post")
    public R listPost(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer status
    ) {
        return postService.listAdminPost(pageNum, pageSize, status);
    }

    // 管理员帖子详情
    @GetMapping("/admin/post/detail")
    public R postDetail(@RequestParam Long id) {
        return postService.adminPostDetail(id);
    }

    // 管理员用户列表
    @GetMapping("/admin/user")
    public R adminUserPage(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return consumerService.adminUserPage(pageNum, pageSize, keyword, status);
    }

    // 管理员禁用/解禁用户（status: 0禁用，1正常）
    @PostMapping("/admin/user/status")
    public R adminUpdateUserStatus(@RequestParam Integer userId, @RequestParam Integer status) {
        return consumerService.adminUpdateUserStatus(userId, status);
    }

    // 管理员评论列表
    @GetMapping("/admin/comment")
    public R adminCommentPage(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return commentService.adminCommentPage(pageNum, pageSize, keyword);
    }

    // 管理员删除评论
    @PostMapping("/admin/comment/delete")
    public R adminDeleteComment(@RequestParam Integer id) {
        return commentService.adminDeleteComment(id);
    }

    // 审核帖子（status: 1通过,2驳回）
    @PostMapping("/admin/post/audit")
    public R auditPost(@RequestBody AdminPostAuditRequest request) {
        return postService.auditPost(request);
    }

    // 下架帖子
    @PostMapping("/admin/post/offline")
    public R offlinePost(@RequestBody AdminPostAuditRequest request) {
        return postService.offlinePost(request == null ? null : request.getPostId());
    }
}
