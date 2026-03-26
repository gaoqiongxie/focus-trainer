package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.UserAbility;

import java.util.List;

/**
 * 能力评估服务
 */
public interface EvaluationService {

    /**
     * 初始化评估
     */
    void initEvaluation(Long userId);

    /**
     * 生成评估结果
     */
    UserAbility generateEvaluation(Long userId);

    /**
     * 获取用户评估结果
     */
    UserAbility getLatestEvaluation(Long userId);

    /**
     * 获取评估历史
     */
    List<UserAbility> getEvaluationHistory(Long userId, int limit);
}
