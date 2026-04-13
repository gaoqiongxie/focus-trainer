package com.focuskids.trainer.service;

import java.util.List;
import java.util.Map;

/**
 * 个性化难度推荐服务
 */
public interface DifficultyRecommendationService {

    /**
     * 获取推荐训练配置
     * @param userId 用户ID
     * @return 推荐列表（每条包含 configId, trainingType, trainingName, level, reason）
     */
    List<Map<String, Object>> getRecommendations(Long userId);

    /**
     * 获取指定训练类型的推荐难度
     * @param userId 用户ID
     * @param trainingType 训练类型
     * @return 推荐配置（configId, level, reason）
     */
    Map<String, Object> recommendForType(Long userId, Integer trainingType);
}
