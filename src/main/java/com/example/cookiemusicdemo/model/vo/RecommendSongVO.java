package com.example.cookiemusicdemo.model.vo;

import com.example.cookiemusicdemo.model.domain.Song;
import lombok.Data;

import java.io.Serializable;

@Data
public class RecommendSongVO implements Serializable {
    private Song song;
    private Double score;
}

