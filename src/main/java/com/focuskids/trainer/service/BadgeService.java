package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.Badge;

import java.util.List;
import java.util.Map;

/**
 * 徽章服务
 */
public interface BadgeService {

    /**
     * 获取用户所有徽章（含已获得/未获得）
     */
    List<Map<String, Object>> getUserBadges(Long userId);

    /**
     * 训练完成后检查并解锁徽章
     * @return 新解锁的徽章列表
     */
    List<Badge> checkAndUnlockAfterTraining(Long userId, Long trainingRecordId);

    /**
     * 打卡后检查并解锁连续打卡徽章
     * @return 新解锁的徽章列表
     */
    List<Badge> checkAndUnlockAfterStreak(Long userId, int streakDays);

    /**
     * 获取用户已解锁的徽章数量
     */
    int getEarnedCount(Long userId);
}
