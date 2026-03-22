package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.CommentMapper;
import com.example.cookiemusicdemo.mapper.ConsumerMapper;
import com.example.cookiemusicdemo.mapper.ListSongMapper;
import com.example.cookiemusicdemo.mapper.PlayRecordMapper;
import com.example.cookiemusicdemo.mapper.PostMapper;
import com.example.cookiemusicdemo.mapper.SongListMapper;
import com.example.cookiemusicdemo.mapper.SongMapper;
import com.example.cookiemusicdemo.model.domain.Consumer;
import com.example.cookiemusicdemo.model.domain.Song;
import com.example.cookiemusicdemo.model.domain.SongList;
import com.example.cookiemusicdemo.model.vo.AdminDashboardStats;
import com.example.cookiemusicdemo.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private ConsumerMapper consumerMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private SongMapper songMapper;
    @Autowired
    private SongListMapper songListMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ListSongMapper listSongMapper;
    @Autowired
    private PlayRecordMapper playRecordMapper;

    private long countSongOnline() {
        QueryWrapper<Song> w = new QueryWrapper<>();
        w.nested(x -> x.eq("status", 1).or().isNull("status"));
        return songMapper.selectCount(w);
    }

    private long countSongOffline() {
        QueryWrapper<Song> w = new QueryWrapper<>();
        w.eq("status", 0);
        return songMapper.selectCount(w);
    }

    private long countSongListOnline() {
        QueryWrapper<SongList> w = new QueryWrapper<>();
        w.nested(x -> x.eq("status", 1).or().isNull("status"));
        return songListMapper.selectCount(w);
    }

    private long countSongListOffline() {
        QueryWrapper<SongList> w = new QueryWrapper<>();
        w.eq("status", 0);
        return songListMapper.selectCount(w);
    }

    @Override
    public R adminStats() {
        AdminDashboardStats s = new AdminDashboardStats();

        s.setUserTotal(consumerMapper.selectCount(null));
        QueryWrapper<Consumer> disabled = new QueryWrapper<>();
        disabled.eq("status", 0);
        s.setUserDisabled(consumerMapper.selectCount(disabled));

        s.setPostTotal(postMapper.countAdminPosts(null));
        s.setPostPending(postMapper.countAdminPosts(0));
        s.setPostApproved(postMapper.countAdminPosts(1));
        s.setPostRejected(postMapper.countAdminPosts(2));
        s.setPostOffline(postMapper.countAdminPosts(3));

        s.setSongTotal(songMapper.selectCount(null));
        s.setSongOnline(countSongOnline());
        s.setSongOffline(countSongOffline());

        s.setSongListTotal(songListMapper.selectCount(null));
        s.setSongListOnline(countSongListOnline());
        s.setSongListOffline(countSongListOffline());

        s.setCommentTotal(commentMapper.selectCount(null));
        s.setListSongTotal(listSongMapper.selectCount(null));
        s.setPlayRecordTotal(playRecordMapper.selectCount(null));

        return R.success("看板统计", s);
    }
}
