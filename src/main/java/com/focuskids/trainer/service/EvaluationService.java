package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.UserAbility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 能力评估服务
 */
public interface EvaluationService {

    /**
     * 初始化评估（首次进入引导时调用）
     */
    void initEvaluation(Long userId);

    /**
     * 生成/更新能力评估结果（基于历史训练数据计算）
     */
    UserAbility generateEvaluation(Long userId);

    /**
     * 获取最新评估结果
     */
    UserAbility getLatestEvaluation(Long userId);

    /**
     * 获取评估历史
     */
    List<UserAbility> getEvaluationHistory(Long userId, int limit);

    /**
     * 计算用户能力各维度得分
     */
    BigDecimal[] calculateAbilityScores(Long userId);

    /**
     * 根据能力评估推荐训练类型（薄弱项优先）
     */
    List<Map<String, Object>> getRecommendations(UserAbility ability);
}
