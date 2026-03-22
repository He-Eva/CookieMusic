package com.example.cookiemusicdemo.controller;

import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.request.AdminPostAuditRequest;
import com.example.cookiemusicdemo.model.request.AdminRequest;
import com.example.cookiemusicdemo.model.request.SongListRequest;
import com.example.cookiemusicdemo.service.AdminService;
import com.example.cookiemusicdemo.service.DashboardService;
import com.example.cookiemusicdemo.service.CommentService;
import com.example.cookiemusicdemo.service.ConsumerService;
import com.example.cookiemusicdemo.model.request.SongRequest;
import com.example.cookiemusicdemo.service.ListSongService;
import com.example.cookiemusicdemo.service.PostService;
import com.example.cookiemusicdemo.service.SongListService;
import com.example.cookiemusicdemo.service.SongService;
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
    @Autowired
    private SongService songService;
    @Autowired
    private SongListService songListService;
    @Autowired
    private ListSongService listSongService;
    @Autowired
    private DashboardService dashboardService;

    // 管理端数据看板统计
    @GetMapping("/admin/dashboard/stats")
    public R dashboardStats() {
        return dashboardService.adminStats();
    }

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

    // 管理员歌曲分页（keyword 匹配歌名或歌手名；status 0下架 1上架）
    @GetMapping("/admin/song/page")
    public R adminSongPage(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return songService.adminSongPage(pageNum, pageSize, keyword, status);
    }

    // 管理员歌曲详情（含歌词）
    @GetMapping("/admin/song/detail")
    public R adminSongDetail(@RequestParam Integer id) {
        return songService.adminSongDetail(id);
    }

    // 管理员上架/下架歌曲
    @PostMapping("/admin/song/status")
    public R adminSongStatus(@RequestParam Integer songId, @RequestParam Integer status) {
        return songService.adminUpdateSongStatus(songId, status);
    }

    // 管理员更新歌名、简介、歌词
    @PostMapping("/admin/song/update")
    public R adminSongUpdate(@RequestBody SongRequest request) {
        return songService.adminUpdateSongInfo(request);
    }

    // ---------- 歌单管理 ----------
    @GetMapping("/admin/songList/page")
    public R adminSongListPage(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return songListService.adminSongListPage(pageNum, pageSize, keyword, status);
    }

    @GetMapping("/admin/songList/detail")
    public R adminSongListDetail(@RequestParam Integer id) {
        return songListService.adminSongListDetail(id);
    }

    @PostMapping("/admin/songList/add")
    public R adminSongListAdd(@RequestBody SongListRequest request) {
        return songListService.adminAddSongList(request);
    }

    @PostMapping("/admin/songList/update")
    public R adminSongListUpdate(@RequestBody SongListRequest request) {
        return songListService.adminUpdateSongList(request);
    }

    @PostMapping("/admin/songList/delete")
    public R adminSongListDelete(@RequestParam Integer id) {
        return songListService.adminDeleteSongList(id);
    }

    @PostMapping("/admin/songList/status")
    public R adminSongListStatus(@RequestParam Integer songListId, @RequestParam Integer status) {
        return songListService.adminUpdateSongListStatus(songListId, status);
    }

    /** 歌单内曲目列表（list_song 记录） */
    @GetMapping("/admin/songList/songs")
    public R adminSongsInSongList(@RequestParam Integer songListId) {
        return listSongService.listSongOfSongId(songListId);
    }

    @PostMapping("/admin/songList/song/add")
    public R adminAddSongToSongList(@RequestParam Integer songListId, @RequestParam Integer songId) {
        return listSongService.adminAddSongToList(songListId, songId);
    }

    @PostMapping("/admin/songList/song/remove")
    public R adminRemoveSongFromSongList(@RequestParam Integer songListId, @RequestParam Integer songId) {
        return listSongService.adminRemoveSongFromList(songListId, songId);
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
