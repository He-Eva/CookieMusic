package com.example.cookiemusicdemo.service;

import com.example.cookiemusicdemo.common.R;

public interface RecommendService {
    R recommendSongs(Integer consumerId, Integer limit);
}

