package com.example.cookiemusicdemo.controller;

import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.model.request.SongListRequest;
import com.example.cookiemusicdemo.service.SongListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SongListController {

    @Autowired
    private SongListService songListService;


    // 添加歌单
    @PostMapping("/songList/add")
    public R addSongList(@RequestBody SongListRequest addSongListRequest) {
        return songListService.addSongList(addSongListRequest);
    }

    // 删除歌单
    @GetMapping("/songList/delete")
    public R deleteSongList(@RequestParam int id) {
        return songListService.deleteSongList(id);
    }

    //TODO 这块就是前端显现相应的歌单list
    // 返回所有歌单
    @GetMapping("/songList")
    public R allSongList() {
        return songListService.allSongList();
    }

    // 返回标题包含文字的歌单
    @GetMapping("/songList/likeTitle/detail")
    public R songListOfLikeTitle(@RequestParam String title) {
        return songListService.likeTitle('%' + title + '%');
    }

    // 返回指定类型的歌单
    @GetMapping("/songList/style/detail")
    public R songListOfStyle(@RequestParam String style) {
        return songListService.likeStyle('%' + style + '%');
    }

    // 返回指定ID的歌单
    @GetMapping("/songList/detail")
    public R songListOfId(@RequestParam Integer id) {
        return songListService.songListOfId(id);
    }

    // 更新歌单信息
    @PostMapping("/songList/update")
    public R updateSongListMsg(@RequestBody SongListRequest updateSongListRequest) {
        return songListService.updateSongListMsg(updateSongListRequest);

    }

    // 更新歌单图片
    @PostMapping("/songList/img/update")
    public R updateSongListPic(@RequestParam("file") MultipartFile avatorFile, @RequestParam("id") int id) {

        return songListService.updateSongListImg(avatorFile,id);
    }
}
