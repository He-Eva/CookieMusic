package com.example.cookiemusicdemo.model.vo;

import lombok.Data;

/**
 * 管理端首页看板统计
 */
@Data
public class AdminDashboardStats {

    private long userTotal;
    private long userDisabled;

    private long postTotal;
    private long postPending;
    private long postApproved;
    private long postRejected;
    private long postOffline;

    private long songTotal;
    private long songOnline;
    private long songOffline;

    private long songListTotal;
    private long songListOnline;
    private long songListOffline;

    private long commentTotal;
    /** 歌单-歌曲关联条数 */
    private long listSongTotal;
    private long playRecordTotal;
}
