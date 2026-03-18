package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.RecommendMapper;
import com.example.cookiemusicdemo.mapper.SongMapper;
import com.example.cookiemusicdemo.model.domain.Song;
import com.example.cookiemusicdemo.model.vo.RecommendSongVO;
import com.example.cookiemusicdemo.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private RecommendMapper recommendMapper;

    @Autowired
    private SongMapper songMapper;

    /**
     * Weights for implicit feedback.
     */
    private static final double W_PLAY = 1.0;
    private static final double W_COLLECT = 3.0;
    private static final double W_RATING = 1.5;

    /**
     * Limits to keep demo performant.
     */
    private static final int USER_SEED_TOP_K = 30;   // use user's top K songs as seeds
    private static final int SIM_NEIGHBOR_K = 50;    // for each seed, take top K similar items

    @Override
    @Cacheable(
            value = "recommend",
            key = "T(String).valueOf(#consumerId) + ':' + T(String).valueOf(#limit)",
            unless = "#result == null || #result.success == false"
    )
    public R recommendSongs(Integer consumerId, Integer limit) {
        if (consumerId == null) return R.error("参数错误");
        int topN = (limit == null || limit < 1 || limit > 50) ? 10 : limit;

        // 1) Build user->(song->preferenceScore) from plays + collects + ratings.
        Map<Integer, Map<Integer, Double>> userItem = buildUserSongPreference();

        Map<Integer, Double> targetVector = userItem.getOrDefault(consumerId, Collections.emptyMap());
        if (targetVector.isEmpty()) {
            // cold-start fallback: return latest songs (or you can choose hot songs)
            List<Song> latest = songMapper.selectList(new QueryWrapper<Song>().orderByDesc("id").last("LIMIT " + topN));
            return R.success("推荐（冷启动：最新歌曲）", latest);
        }

        // 2) Compute item-item similarity using cosine on implicit scores.
        Map<Integer, Map<Integer, Double>> itemSims = computeItemSimilarities(userItem);

        // 3) Rank candidates for target user.
        Set<Integer> seen = targetVector.keySet();
        List<Integer> seedItems = targetVector.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(USER_SEED_TOP_K)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<Integer, Double> candidateScore = new HashMap<>();
        for (Integer seed : seedItems) {
            Map<Integer, Double> sims = itemSims.getOrDefault(seed, Collections.emptyMap());
            // take top neighbors
            sims.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(SIM_NEIGHBOR_K)
                    .forEach(e -> {
                        Integer item = e.getKey();
                        if (seen.contains(item)) return;
                        double sim = e.getValue();
                        double weight = targetVector.getOrDefault(seed, 0.0);
                        candidateScore.merge(item, sim * weight, Double::sum);
                    });
        }

        if (candidateScore.isEmpty()) {
            List<Song> latest = songMapper.selectList(new QueryWrapper<Song>().orderByDesc("id").last("LIMIT " + topN));
            return R.success("推荐（兜底：最新歌曲）", latest);
        }

        List<Map.Entry<Integer, Double>> top = candidateScore.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .collect(Collectors.toList());

        List<Integer> songIds = top.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        // fetch songs and preserve order
        List<Song> songs = songMapper.selectBatchIds(songIds);
        Map<Integer, Song> byId = songs.stream().collect(Collectors.toMap(Song::getId, s -> s, (a, b) -> a));
        List<RecommendSongVO> result = new ArrayList<>();
        // Normalize scores to make them more human-friendly (0~100) and reduce ties for demo readability.
        double minScore = Double.POSITIVE_INFINITY;
        double maxScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Integer, Double> e : top) {
            double v = e.getValue() == null ? 0.0 : e.getValue();
            if (v < minScore) minScore = v;
            if (v > maxScore) maxScore = v;
        }

        for (int idx = 0; idx < top.size(); idx++) {
            Map.Entry<Integer, Double> e = top.get(idx);
            Song s = byId.get(e.getKey());
            if (s == null) continue;
            RecommendSongVO vo = new RecommendSongVO();
            vo.setSong(s);

            double raw = e.getValue() == null ? 0.0 : e.getValue();
            double normalized;
            if (!Double.isFinite(raw) || !Double.isFinite(minScore) || !Double.isFinite(maxScore)) {
                normalized = 0.0;
            } else if (Math.abs(maxScore - minScore) < 1e-12) {
                // If all candidates have the same raw score, keep ordering stable but avoid identical display scores.
                normalized = 100.0 - idx * 0.01;
            } else {
                normalized = (raw - minScore) / (maxScore - minScore) * 100.0;
            }
            // Even with normalization, top items can still tie at 100/0. Add a tiny deterministic decay by rank
            // to make the demo output easier to read, without changing the actual ordering.
            normalized = normalized - idx * 0.001;
            if (normalized < 0) normalized = 0.0;
            if (normalized > 100) normalized = 100.0;
            // round to 4 decimals (enough to see differences, not too noisy)
            normalized = Math.round(normalized * 10000.0) / 10000.0;
            vo.setScore(normalized);
            result.add(vo);
        }

        return R.success("推荐成功", result);
    }

    private Map<Integer, Map<Integer, Double>> buildUserSongPreference() {
        Map<Integer, Map<Integer, Double>> userItem = new HashMap<>();

        // plays: use log(1+count) to reduce heavy user bias
        List<Map<String, Object>> plays = recommendMapper.selectUserSongPlayCounts(90);
        for (Map<String, Object> row : plays) {
            Integer u = toInt(row.get("consumerId"));
            Integer i = toInt(row.get("songId"));
            Integer c = toInt(row.get("playCount"));
            if (u == null || i == null || c == null) continue;
            double s = W_PLAY * Math.log(1.0 + c);
            userItem.computeIfAbsent(u, k -> new HashMap<>()).merge(i, s, Double::sum);
        }

        // collects: strong positive feedback
        List<Map<String, Object>> collects = recommendMapper.selectUserSongCollects();
        for (Map<String, Object> row : collects) {
            Integer u = toInt(row.get("consumerId"));
            Integer i = toInt(row.get("songId"));
            if (u == null || i == null) continue;
            userItem.computeIfAbsent(u, k -> new HashMap<>()).merge(i, W_COLLECT, Double::sum);
        }

        // ratings on songlists: distribute score to songs in the list
        List<Map<String, Object>> ratings = recommendMapper.selectUserSongFromSongListRatings(1);
        for (Map<String, Object> row : ratings) {
            Integer u = toInt(row.get("consumerId"));
            Integer i = toInt(row.get("songId"));
            Integer score = toInt(row.get("score"));
            if (u == null || i == null || score == null) continue;
            // normalize to 0~1 then weight
            double s = W_RATING * (score / 10.0);
            userItem.computeIfAbsent(u, k -> new HashMap<>()).merge(i, s, Double::sum);
        }

        return userItem;
    }

    /**
     * Cosine similarity over implicit scores.
     * sim(i,j) = dot(i,j) / (||i|| * ||j||)
     */
    private Map<Integer, Map<Integer, Double>> computeItemSimilarities(Map<Integer, Map<Integer, Double>> userItem) {
        // norm: item -> sum(score^2)
        Map<Integer, Double> norm = new HashMap<>();
        // co-occurrence dot: item -> (item2 -> dot)
        Map<Integer, Map<Integer, Double>> dot = new HashMap<>();

        for (Map<Integer, Double> items : userItem.values()) {
            List<Map.Entry<Integer, Double>> list = new ArrayList<>(items.entrySet());
            // update norms
            for (Map.Entry<Integer, Double> e : list) {
                norm.merge(e.getKey(), e.getValue() * e.getValue(), Double::sum);
            }
            // update pairwise dot products
            for (int a = 0; a < list.size(); a++) {
                int i = list.get(a).getKey();
                double si = list.get(a).getValue();
                for (int b = a + 1; b < list.size(); b++) {
                    int j = list.get(b).getKey();
                    double sj = list.get(b).getValue();
                    double v = si * sj;
                    dot.computeIfAbsent(i, k -> new HashMap<>()).merge(j, v, Double::sum);
                    dot.computeIfAbsent(j, k -> new HashMap<>()).merge(i, v, Double::sum);
                }
            }
        }

        Map<Integer, Map<Integer, Double>> sims = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Double>> e : dot.entrySet()) {
            int i = e.getKey();
            double ni = Math.sqrt(norm.getOrDefault(i, 0.0));
            if (ni == 0) continue;
            Map<Integer, Double> m = new HashMap<>();
            for (Map.Entry<Integer, Double> e2 : e.getValue().entrySet()) {
                int j = e2.getKey();
                double nj = Math.sqrt(norm.getOrDefault(j, 0.0));
                if (nj == 0) continue;
                double sim = e2.getValue() / (ni * nj);
                if (Double.isFinite(sim) && sim > 0) {
                    m.put(j, sim);
                }
            }
            if (!m.isEmpty()) sims.put(i, m);
        }
        return sims;
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Long) return ((Long) o).intValue();
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}

