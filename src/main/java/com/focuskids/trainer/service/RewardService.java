package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.RewardRecord;
import com.focuskids.trainer.entity.UserStreak;

import java.util.List;
import java.util.Map;

/**
 * 激励服务
 */
public interface RewardService {

    /**
     * 获取用户星星总数
     */
    int getStarCount(Long userId);

    /**
     * 增加星星
     */
    void addStars(Long userId, int count, Integer sourceType, Long sourceId);

    /**
     * 获取徽章列表
     */
    List<Map<String, Object>> getBadges(Long userId);

    /**
     * 获取连续打卡记录
     */
    UserStreak getStreak(Long userId);

    /**
     * 更新连续打卡
     */
    UserStreak updateStreak(Long userId);
}
