package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.controller.MinioUploadController;
import com.example.cookiemusicdemo.mapper.ListSongMapper;
import com.example.cookiemusicdemo.mapper.SongListMapper;
import com.example.cookiemusicdemo.model.domain.ListSong;
import com.example.cookiemusicdemo.model.domain.SongList;
import com.example.cookiemusicdemo.model.request.SongListRequest;
import com.example.cookiemusicdemo.service.SongListService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SongListServiceImpl extends ServiceImpl<SongListMapper, SongList> implements SongListService {

    @Autowired
    private SongListMapper songListMapper;
    @Autowired
    private ListSongMapper listSongMapper;
    @Value("${minio.bucket-name}")
    String bucketName;

    private void applyOnlineOnly(QueryWrapper<SongList> wrapper) {
        wrapper.and(w -> w.eq("status", 1).or().isNull("status"));
    }

    @Override
    public R updateSongListMsg(SongListRequest updateSongListRequest) {
        SongList songList = new SongList();
        BeanUtils.copyProperties(updateSongListRequest, songList);
        if (songListMapper.updateById(songList) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteSongList(Integer id) {
        if (id == null) return R.error("参数错误");
        QueryWrapper<ListSong> lw = new QueryWrapper<>();
        lw.eq("song_list_id", id);
        listSongMapper.delete(lw);
        if (songListMapper.deleteById(id) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    @Override
    public R allSongList() {
        QueryWrapper<SongList> qw = new QueryWrapper<>();
        applyOnlineOnly(qw);
        qw.orderByDesc("id");
        return R.success(null, songListMapper.selectList(qw));
    }

    @Override
    public R songListOfId(Integer id) {
        if (id == null) return R.error("参数错误");
        SongList songList = songListMapper.selectById(id);
        if (songList == null) return R.success(null, Collections.emptyList());
        if (songList.getStatus() != null && songList.getStatus() == 0) {
            return R.error("歌单已下架");
        }
        // 保持与其它 detail 接口一致：返回 List，前端取 data[0]
        return R.success(null, Collections.singletonList(songList));
    }

    @Override
    public List<SongList> findAllSong() {
        List<SongList> songLists = songListMapper.selectList(null);
        return songLists;
    }


    @Override
    public R likeTitle(String title) {
        QueryWrapper<SongList> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("title",title);
        applyOnlineOnly(queryWrapper);
        return R.success(null, songListMapper.selectList(queryWrapper));
    }

    @Override
    public R likeStyle(String style) {
        QueryWrapper<SongList> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("style",style);
        applyOnlineOnly(queryWrapper);
        return R.success(null, songListMapper.selectList(queryWrapper));
    }

    @Override
    public R addSongList(SongListRequest addSongListRequest) {
        SongList songList = new SongList();
        BeanUtils.copyProperties(addSongListRequest, songList);
        String pic = "/user01/songlist/img/default.jpg";
        songList.setPic(pic);
        songList.setStatus((byte) 1);
        if (songListMapper.insert(songList) > 0) {
            return R.success("添加成功");
        } else {
            return R.error("添加失败");
        }
    }

    @Override
    public R updateSongListImg(MultipartFile avatorFile, @RequestParam("id") int id) {
        String fileName =avatorFile.getOriginalFilename();
        String path="/"+bucketName+"/"+"songlist/img/";
        String imgPath = path + fileName;
        MinioUploadController.uploadSonglistImgFile(avatorFile);
        SongList songList = new SongList();
        songList.setId(id);
        songList.setPic(imgPath);
        if (songListMapper.updateById(songList) > 0) {
            return R.success("上传成功", imgPath);
        } else {
            return R.error("上传失败");
        }
    }

    @Override
    public R adminSongListPage(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pn - 1) * ps;
        Integer queryStatus = (status != null && (status < 0 || status > 1)) ? null : status;
        Map<String, Object> data = new HashMap<>();
        data.put("items", songListMapper.selectAdminSongListPage(offset, ps, keyword, queryStatus));
        data.put("total", songListMapper.countAdminSongLists(keyword, queryStatus));
        data.put("pageNum", pn);
        data.put("pageSize", ps);
        return R.success("管理员歌单列表", data);
    }

    @Override
    public R adminSongListDetail(Integer id) {
        if (id == null) return R.error("参数错误");
        SongList sl = songListMapper.selectById(id);
        if (sl == null) return R.error("歌单不存在");
        return R.success("歌单详情", sl);
    }

    @Override
    public R adminAddSongList(SongListRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return R.error("标题不能为空");
        }
        return addSongList(request);
    }

    @Override
    public R adminUpdateSongList(SongListRequest request) {
        return updateSongListMsg(request);
    }

    @Override
    public R adminDeleteSongList(Integer id) {
        return deleteSongList(id);
    }

    @Override
    public R adminUpdateSongListStatus(Integer songListId, Integer status) {
        if (songListId == null || status == null || (status != 0 && status != 1)) {
            return R.error("参数错误");
        }
        SongList db = songListMapper.selectById(songListId);
        if (db == null) return R.error("歌单不存在");
        SongList u = new SongList();
        u.setId(songListId);
        u.setStatus((byte) status.intValue());
        if (songListMapper.updateById(u) > 0) {
            return R.success(status == 1 ? "已上架" : "已下架");
        }
        return R.error("操作失败");
    }
}
