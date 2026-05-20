package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.mapper.RecommendMapper;
import com.example.cookiemusicdemo.mapper.SongMapper;
import com.example.cookiemusicdemo.model.domain.Song;
import com.example.cookiemusicdemo.model.vo.RecommendSongVO;
import com.example.cookiemusicdemo.service.RecommendService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化歌曲推荐服务（基于物品的协同过滤 ItemCF）。
 * <p>
 * 数据来源：播放记录、歌曲收藏、歌单评分（经 list_song 展开到具体歌曲）。
 * 算法概要：先构建「用户-歌曲」隐式偏好矩阵，再计算歌曲间余弦相似度，
 * 用当前用户偏好最高的若干首歌作为种子，聚合相似歌曲得分后排序推荐。
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private RecommendMapper recommendMapper;

    @Autowired
    private SongMapper songMapper;

    /** 隐式反馈权重：播放 */
    private static final double W_PLAY = 1.0;
    /** 隐式反馈权重：歌曲收藏（信号最强） */
    private static final double W_COLLECT = 3.0;
    /** 隐式反馈权重：歌单评分（展开到歌单内每首歌） */
    private static final double W_RATING = 1.5;

    /** 种子歌曲数量：取用户偏好最高的 Top-K 首作为推荐起点 */
    private static final int USER_SEED_TOP_K = 30;
    /** 每个种子最多参考的相似邻居数量，控制计算量 */
    private static final int SIM_NEIGHBOR_K = 50;

    /**
     * 为指定用户推荐歌曲。
     * 结果缓存 5 分钟（Redis 键 recommend），键为 consumerId:limit。
     */
    @Override
    @Cacheable(
            value = "recommend",
            key = "T(String).valueOf(#consumerId) + ':' + T(String).valueOf(#limit)",
            unless = "#result == null || #result.success == false"
    )
    public R recommendSongs(Integer consumerId, Integer limit) {
        if (consumerId == null) return R.error("参数错误");
        // 默认推荐 10 首，上限 50
        int topN = (limit == null || limit < 1 || limit > 50) ? 10 : limit;

        // 步骤1：从播放、收藏、歌单评分构建全局「用户 -> (歌曲 -> 偏好分)」矩阵
        Map<Integer, Map<Integer, Double>> userItem = buildUserSongPreference();

        // 当前目标用户的偏好向量（歌曲 id -> 偏好分）
        Map<Integer, Double> targetVector = userItem.getOrDefault(consumerId, Collections.emptyMap());
        if (targetVector.isEmpty()) {
            // 冷启动：新用户无任何行为，返回最新可播放歌曲
            List<Song> latest = filterPlayable(songMapper.selectList(
                    new QueryWrapper<Song>()
                            .nested(w -> w.eq("status", 1).or().isNull("status"))
                            .orderByDesc("id")
                            .last("LIMIT " + (topN * 3))));
            return R.success("推荐（冷启动：最新歌曲）", latest.stream().limit(topN).collect(Collectors.toList()));
        }

        // 步骤2：基于所有用户偏好，计算歌曲与歌曲之间的余弦相似度
        Map<Integer, Map<Integer, Double>> itemSims = computeItemSimilarities(userItem);

        // 步骤3：用 ItemCF 为目标用户打分并排序
        // 用户已有过偏好的歌曲不再推荐（避免重复推荐已听/已收藏的歌）
        Set<Integer> seen = targetVector.keySet();
        // 种子：偏好分最高的 USER_SEED_TOP_K 首歌
        List<Integer> seedItems = targetVector.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(USER_SEED_TOP_K)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 候选歌曲累计得分：对多个种子的「相似度 × 种子偏好」求和
        Map<Integer, Double> candidateScore = new HashMap<>();
        for (Integer seed : seedItems) {
            Map<Integer, Double> sims = itemSims.getOrDefault(seed, Collections.emptyMap());
            // 每个种子只取相似度最高的 SIM_NEIGHBOR_K 个邻居，避免全表扫描
            sims.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(SIM_NEIGHBOR_K)
                    .forEach(e -> {
                        Integer item = e.getKey();
                        if (seen.contains(item)) return;
                        double sim = e.getValue();
                        double weight = targetVector.getOrDefault(seed, 0.0);
                        // 经典 ItemCF 打分：sum( sim(seed, item) * pref(user, seed) )
                        candidateScore.merge(item, sim * weight, Double::sum);
                    });
        }

        if (candidateScore.isEmpty()) {
            // 兜底：有行为但算不出候选（例如曲库孤立），仍返回最新歌曲
            List<Song> latest = filterPlayable(songMapper.selectList(
                    new QueryWrapper<Song>()
                            .nested(w -> w.eq("status", 1).or().isNull("status"))
                            .orderByDesc("id")
                            .last("LIMIT " + (topN * 3))));
            return R.success("推荐（兜底：最新歌曲）", latest.stream().limit(topN).collect(Collectors.toList()));
        }

        // 按候选分降序取 TopN
        List<Map.Entry<Integer, Double>> top = candidateScore.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .collect(Collectors.toList());

        List<Integer> songIds = top.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        // 批量查库后按推荐顺序组装（selectBatchIds 不保证顺序）
        List<Song> songs = songMapper.selectBatchIds(songIds);
        Map<Integer, Song> byId = songs.stream().collect(Collectors.toMap(Song::getId, s -> s, (a, b) -> a));
        List<RecommendSongVO> result = new ArrayList<>();

        // 将原始候选分线性归一化到 0~100，便于前端展示「匹配度」
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
            if (!isPlayableSong(s)) continue;
            RecommendSongVO vo = new RecommendSongVO();
            vo.setSong(s);

            double raw = e.getValue() == null ? 0.0 : e.getValue();
            double normalized;
            if (!Double.isFinite(raw) || !Double.isFinite(minScore) || !Double.isFinite(maxScore)) {
                normalized = 0.0;
            } else if (Math.abs(maxScore - minScore) < 1e-12) {
                // 所有候选原始分相同：按排名微调展示分，避免界面全是 100
                normalized = 100.0 - idx * 0.01;
            } else {
                normalized = (raw - minScore) / (maxScore - minScore) * 100.0;
            }
            // 按名次做极小衰减，仅影响展示数值，不改变排序
            normalized = normalized - idx * 0.001;
            if (normalized < 0) normalized = 0.0;
            if (normalized > 100) normalized = 100.0;
            normalized = Math.round(normalized * 10000.0) / 10000.0;
            vo.setScore(normalized);
            result.add(vo);
        }

        return R.success("推荐成功", result);
    }

    /**
     * 构建全局用户-歌曲偏好矩阵。
     * <p>
     * 结构：userItem.get(用户id).get(歌曲id) = 偏好分（三类行为加权累加）。
     */
    private Map<Integer, Map<Integer, Double>> buildUserSongPreference() {
        Map<Integer, Map<Integer, Double>> userItem = new HashMap<>();

        // 播放：近 90 天，按 (用户, 歌曲) 聚合次数；log(1+次数) 削弱单曲刷屏
        List<Map<String, Object>> plays = recommendMapper.selectUserSongPlayCounts(90);
        for (Map<String, Object> row : plays) {
            Integer u = toInt(row.get("consumerId"));
            Integer i = toInt(row.get("songId"));
            Integer c = toInt(row.get("playCount"));
            if (u == null || i == null || c == null) continue;
            double s = W_PLAY * Math.log(1.0 + c);
            userItem.computeIfAbsent(u, k -> new HashMap<>()).merge(i, s, Double::sum);
        }

        // 歌曲收藏：collect 表 type=0，每条固定加 W_COLLECT
        List<Map<String, Object>> collects = recommendMapper.selectUserSongCollects();
        for (Map<String, Object> row : collects) {
            Integer u = toInt(row.get("consumerId"));
            Integer i = toInt(row.get("songId"));
            if (u == null || i == null) continue;
            userItem.computeIfAbsent(u, k -> new HashMap<>()).merge(i, W_COLLECT, Double::sum);
        }

        // 歌单评分：rank_list 联 list_song，把歌单分摊到歌单内每首歌；仅 score>=1
        List<Map<String, Object>> ratings = recommendMapper.selectUserSongFromSongListRatings(1);
        for (Map<String, Object> row : ratings) {
            Integer u = toInt(row.get("consumerId"));
            Integer i = toInt(row.get("songId"));
            Integer score = toInt(row.get("score"));
            if (u == null || i == null || score == null) continue;
            // 假定评分为 1~10，先归一化到 0~1 再乘权重
            double s = W_RATING * (score / 10.0);
            userItem.computeIfAbsent(u, k -> new HashMap<>()).merge(i, s, Double::sum);
        }

        return userItem;
    }

    /**
     * 计算歌曲-歌曲余弦相似度（基于隐式偏好，非显式星级）。
     * <p>
     * 公式：sim(i,j) = dot(i,j) / (||i|| × ||j||)
     * <br>
     * dot(i,j) 来自「同一用户同时偏好 i 和 j」时的偏好分乘积累加。
     */
    private Map<Integer, Map<Integer, Double>> computeItemSimilarities(Map<Integer, Map<Integer, Double>> userItem) {
        // 每首歌的模长平方：sum(偏好分²)
        Map<Integer, Double> norm = new HashMap<>();
        // 歌曲对的点积：dot.get(i).get(j)
        Map<Integer, Map<Integer, Double>> dot = new HashMap<>();

        for (Map<Integer, Double> items : userItem.values()) {
            List<Map.Entry<Integer, Double>> list = new ArrayList<>(items.entrySet());
            // 累加各歌曲的 norm
            for (Map.Entry<Integer, Double> e : list) {
                norm.merge(e.getKey(), e.getValue() * e.getValue(), Double::sum);
            }
            // 同一用户偏好列表内两两组合，累加点积（对称：i-j 与 j-i 同时更新）
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

        // 点积除以两首歌的模长，得到余弦相似度
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

    /**
     * 是否可推荐/可播放：未下架且音频 URL 在 MinIO 歌曲目录下。
     */
    private boolean isPlayableSong(Song s) {
        if (s == null) return false;
        if (s.getStatus() != null && s.getStatus() == 0) return false;
        String url = s.getUrl();
        return StringUtils.isNotBlank(url) && url.contains("/user01/song/music/");
    }

    /** 过滤出可播放歌曲列表 */
    private List<Song> filterPlayable(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return Collections.emptyList();
        return songs.stream().filter(this::isPlayableSong).collect(Collectors.toList());
    }

    /** MyBatis 返回 Map 时数值类型可能是 Long，统一转成 Integer */
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
