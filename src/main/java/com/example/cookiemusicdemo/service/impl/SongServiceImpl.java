package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.controller.MinioUploadController;
import com.example.cookiemusicdemo.mapper.SongMapper;
import com.example.cookiemusicdemo.model.domain.Song;
import com.example.cookiemusicdemo.model.request.SongRequest;
import com.example.cookiemusicdemo.service.SongService;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements SongService {

    @Autowired
    private SongMapper songMapper;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Autowired
    MinioClient minioClient;

    /** 前台仅展示上架：status=1 或 status IS NULL（兼容旧数据） */
    private void applyOnlineOnly(QueryWrapper<Song> wrapper) {
        wrapper.and(w -> w.eq("status", 1).or().isNull("status"));
    }

    @Override
    public R allSong() {
        QueryWrapper<Song> qw = new QueryWrapper<>();
        applyOnlineOnly(qw);
        qw.orderByDesc("id");
        return R.success(null, songMapper.selectList(qw));
    }

    @Override
    public R addSong(SongRequest addSongRequest, MultipartFile lrcfile, MultipartFile mpfile){
        Song song = new Song();
        BeanUtils.copyProperties(addSongRequest, song);
        String pic = "/user01/song/img/default.jpg";
        String fileName = mpfile.getOriginalFilename();
        String s = MinioUploadController.uploadFile(mpfile);
        String storeUrlPath = "/"+bucketName+"/song/music/" + fileName;
        song.setCreateTime(new Date());
        song.setUpdateTime(new Date());
        song.setPic(pic);
        song.setUrl(storeUrlPath);
        song.setStatus((byte) 1);

        if (lrcfile!=null&&(song.getLyric().equals("[00:00:00]暂无歌词"))){
            byte[] fileContent = new byte[0];
            try {
                fileContent = lrcfile.getBytes();
                String content = new String(fileContent, StandardCharsets.UTF_8);
                song.setLyric(content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (s.equals("File uploaded successfully!")&&songMapper.insert(song) > 0) {
            return R.success("上传成功", storeUrlPath);
        } else {
            return R.error("上传失败");
        }
    }

    @Override
    public R updateSongMsg(SongRequest updateSongRequest) {
        Song song = new Song();
        BeanUtils.copyProperties(updateSongRequest, song);
        if (songMapper.updateById(song) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    @Override
    public R updateSongUrl(MultipartFile urlFile, int id) {
        Song song = songMapper.selectById(id);
        String path = song.getUrl();
        String[] parts = path.split("/");
        String fileName = parts[parts.length - 1];

        RemoveObjectArgs removeObjectArgs=RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object("/song/music/"+fileName)
                .build();
        fileName = urlFile.getOriginalFilename();
        String s = MinioUploadController.uploadFile(urlFile);
        try {
            minioClient.removeObject(removeObjectArgs);
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        }
        String storeUrlPath = "/"+bucketName+"/" + fileName;
        song.setId(id);
        song.setUrl(storeUrlPath);
        song.setName(fileName);
        if (s.equals("File uploaded successfully!")&&songMapper.updateById(song) > 0) {
            return R.success("更新成功", storeUrlPath);
        } else {
            return R.error("更新失败");
        }
    }

    @Override
    public R updateSongPic(MultipartFile urlFile, int id) {
        String fileName =  urlFile.getOriginalFilename();
        String storeUrlPath = "/user01/song/img/" + fileName;
        MinioUploadController.uploadSongImgFile(urlFile);
        Song song = new Song();
        song.setId(id);
        song.setPic(storeUrlPath);
        if (songMapper.updateById(song) > 0) {
            return R.success("上传成功", storeUrlPath);
        } else {
            return R.error("上传失败");
        }
    }

    @Override
    public R deleteSong(Integer id) {
        Song song = songMapper.selectById(id);
        String path = song.getUrl();
        String[] parts = path.split("/");
        String fileName = parts[parts.length - 1];
        System.out.println(fileName);
        RemoveObjectArgs removeObjectArgs=RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .build();
        if (songMapper.deleteById(id) > 0) {
            try {
                minioClient.removeObject(removeObjectArgs);
            } catch (ErrorResponseException e) {
                throw new RuntimeException(e);
            } catch (InsufficientDataException e) {
                throw new RuntimeException(e);
            } catch (InternalException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (InvalidResponseException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (ServerException e) {
                throw new RuntimeException(e);
            } catch (XmlParserException e) {
                throw new RuntimeException(e);
            }
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    @Override
    public R songOfSingerId(Integer singerId) {
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("singer_id",singerId);
        applyOnlineOnly(queryWrapper);
        return R.success(null, songMapper.selectList(queryWrapper));
    }

    @Override
    public R songOfId(Integer id) {
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        List<Song> list = songMapper.selectList(queryWrapper);
        if (list == null || list.isEmpty()) {
            return R.error("内容不存在");
        }
        Song s = list.get(0);
        if (s.getStatus() != null && s.getStatus() == 0) {
            return R.error("歌曲已下架");
        }
        return R.success(null, list);
    }

    @Override
    public R songOfSingerName(String name) {
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name",name);
        applyOnlineOnly(queryWrapper);
        List<Song> songs = songMapper.selectList(queryWrapper);
        if (songs.isEmpty()){
            return R.error("添加失败，没有找到该歌,无法加入该歌单");
        }

        return R.success(null, songs);
    }

    @Override
    public R updateSongLrc(MultipartFile lrcFile, int id) {
        Song song = songMapper.selectById(id);
        if (lrcFile!=null&&!(song.getLyric().equals("[00:00:00]暂无歌词"))){
            byte[] fileContent = new byte[0];
            try {
                fileContent = lrcFile.getBytes();
                String content = new String(fileContent, "GB2312");
                song.setLyric(content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (songMapper.updateById(song) > 0) {
            return R.success("更新成功");
        } else {
            return R.error("更新失败");
        }
    }

    @Override
    public R adminSongPage(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pn - 1) * ps;
        Integer queryStatus = (status != null && (status < 0 || status > 1)) ? null : status;
        Map<String, Object> data = new HashMap<>();
        data.put("items", songMapper.selectAdminSongPage(offset, ps, keyword, queryStatus));
        data.put("total", songMapper.countAdminSongs(keyword, queryStatus));
        data.put("pageNum", pn);
        data.put("pageSize", ps);
        return R.success("管理员歌曲列表", data);
    }

    @Override
    public R adminSongDetail(Integer id) {
        if (id == null) {
            return R.error("参数错误");
        }
        Song song = songMapper.selectById(id);
        if (song == null) {
            return R.error("歌曲不存在");
        }
        return R.success("歌曲详情", song);
    }

    @Override
    public R adminUpdateSongStatus(Integer songId, Integer status) {
        if (songId == null || status == null || (status != 0 && status != 1)) {
            return R.error("参数错误");
        }
        Song db = songMapper.selectById(songId);
        if (db == null) {
            return R.error("歌曲不存在");
        }
        Song update = new Song();
        update.setId(songId);
        update.setStatus((byte) status.intValue());
        if (songMapper.updateById(update) > 0) {
            return R.success(status == 1 ? "已上架" : "已下架");
        }
        return R.error("操作失败");
    }

    @Override
    public R adminUpdateSongInfo(SongRequest request) {
        if (request == null || request.getId() == null) {
            return R.error("参数错误");
        }
        Song db = songMapper.selectById(request.getId());
        if (db == null) {
            return R.error("歌曲不存在");
        }
        Song song = new Song();
        song.setId(request.getId());
        song.setName(request.getName());
        song.setIntroduction(request.getIntroduction());
        song.setLyric(request.getLyric());
        if (songMapper.updateById(song) > 0) {
            return R.success("保存成功");
        }
        return R.error("保存失败");
    }
}
