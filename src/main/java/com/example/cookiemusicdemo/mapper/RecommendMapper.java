package com.example.cookiemusicdemo.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface RecommendMapper {

    /**
     * Plays aggregated by (consumer_id, song_id)
     * Returns rows with keys: consumerId, songId, playCount
     */
    List<Map<String, Object>> selectUserSongPlayCounts(@Param("days") Integer days);

    /**
     * Song collects (type=0) as implicit positive feedback.
     * Returns rows with keys: consumerId, songId
     */
    List<Map<String, Object>> selectUserSongCollects();

    /**
     * SongList ratings joined to songs in list.
     * Returns rows with keys: consumerId, songId, score
     */
    List<Map<String, Object>> selectUserSongFromSongListRatings(@Param("minScore") Integer minScore);
}

