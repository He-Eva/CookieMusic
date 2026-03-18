package com.example.cookiemusicdemo.controller;

import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /**
     * Recommend songs for a user (consumer).
     * Example: GET /recommend/songs?consumerId=1&limit=10
     */
    @GetMapping("/recommend/songs")
    public R recommendSongs(@RequestParam Integer consumerId, @RequestParam(required = false) Integer limit) {
        return recommendService.recommendSongs(consumerId, limit);
    }
}

