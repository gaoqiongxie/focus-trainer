package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.Badge;

import java.util.List;

/**
 * 徽章规则引擎 — 评估用户是否满足徽章解锁条件
 */
public interface BadgeRuleEngine {

    /**
     * 训练完成后评估所有徽章条件
     * @param userId        用户ID
     * @param trainingRecordId 本次训练记录ID
     * @return 新解锁的徽章列表
     */
    List<Badge> evaluateAfterTraining(Long userId, Long trainingRecordId);

    /**
     * 打卡后评估连续打卡类徽章
     * @param userId    用户ID
     * @param streakDays 当前连续天数
     * @return 新解锁的徽章列表
     */
    List<Badge> evaluateAfterStreak(Long userId, int streakDays);
}
