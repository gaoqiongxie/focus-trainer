package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.entity.*;
import com.focuskids.trainer.mapper.*;
import com.focuskids.trainer.service.BadgeRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeRuleEngineImpl implements BadgeRuleEngine {

    private final BadgeMapper          badgeMapper;
    private final UserBadgeMapper      userBadgeMapper;
    private final TrainingRecordMapper  trainingRecordMapper;
    private final SysUserMapper        userMapper;
    private final UserStreakMapper     userStreakMapper;

    @Override
    public List<Badge> evaluateAfterTraining(Long userId, Long trainingRecordId) {
        List<Badge> newBadges = new ArrayList<>();

        // 1. 查询所有活跃徽章
        List<Badge> allBadges = badgeMapper.selectAllActive();
        if (allBadges.isEmpty()) return newBadges;

        // 2. 查询用户已获得的徽章ID集合
        List<Integer> earnedIds = userBadgeMapper.selectBadgeIdsByUserId(userId);
        Set<Integer> earnedSet = new HashSet<>(earnedIds);

        // 3. 预加载统计数据
        int totalTrainingCount = countCompletedTraining(userId);
        int perfectCount       = countPerfectScore(userId);
        int accuracy90Count     = countAccuracy90(userId);
        int totalStars          = getTotalStars(userId);
        int currentStreak       = getCurrentStreak(userId);

        // 4. 遍历评估每个徽章
        for (Badge badge : allBadges) {
            if (earnedSet.contains(badge.getBadgeId())) continue; // 已获得，跳过

            if (checkCondition(badge, userId, totalTrainingCount, perfectCount,
                               accuracy90Count, totalStars, currentStreak, trainingRecordId)) {
                // 解锁：写入 user_badge
                UserBadge ub = new UserBadge();
                ub.setUserId(userId);
                ub.setBadgeId(badge.getBadgeId());
                ub.setEarnedAt(LocalDateTime.now());
                ub.setSourceType(1); // 1=训练
                ub.setSourceId(trainingRecordId);
                userBadgeMapper.insert(ub);

                newBadges.add(badge);
                log.info("[徽章解锁] userId={}, badgeKey={}, name={}", userId, badge.getBadgeKey(), badge.getName());
            }
        }

        return newBadges;
    }

    @Override
    public List<Badge> evaluateAfterStreak(Long userId, int streakDays) {
        List<Badge> newBadges = new ArrayList<>();

        List<Badge> streakBadges = badgeMapper.selectByCategory("streak");
        if (streakBadges.isEmpty()) return newBadges;

        List<Integer> earnedIds = userBadgeMapper.selectBadgeIdsByUserId(userId);
        Set<Integer> earnedSet = new HashSet<>(earnedIds);

        for (Badge badge : streakBadges) {
            if (earnedSet.contains(badge.getBadgeId())) continue;
            if ("streak_days".equals(badge.getConditionType())
                    && streakDays >= badge.getConditionValue()) {
                UserBadge ub = new UserBadge();
                ub.setUserId(userId);
                ub.setBadgeId(badge.getBadgeId());
                ub.setEarnedAt(LocalDateTime.now());
                ub.setSourceType(2); // 2=打卡
                ub.setSourceId(null);
                userBadgeMapper.insert(ub);
                newBadges.add(badge);
                log.info("[徽章解锁-streak] userId={}, badgeKey={}, streakDays={}",
                          userId, badge.getBadgeKey(), streakDays);
            }
        }

        return newBadges;
    }

    /**
     * 评估单个徽章条件是否满足
     */
    private boolean checkCondition(Badge badge, Long userId,
                                   int totalTrainingCount, int perfectCount,
                                   int accuracy90Count, int totalStars,
                                   int currentStreak, Long trainingRecordId) {
        String type  = badge.getConditionType();
        int    value = badge.getConditionValue();

        switch (type) {
            case "training_count":
                return totalTrainingCount >= value;

            case "streak_days":
                return currentStreak >= value;

            case "perfect_score":
                // 只要有任意一次满分即可（value=1表示至少1次）
                return perfectCount >= 1;

            case "accuracy_90_count":
                return accuracy90Count >= value;

            case "total_stars":
                return totalStars >= value;

            default:
                log.warn("[徽章规则] 未知条件类型: {}", type);
                return false;
        }
    }

    // ==================== 统计辅助方法 ====================

    private int countCompletedTraining(Long userId) {
        LambdaQueryWrapper<TrainingRecord> w = new LambdaQueryWrapper<>();
        w.eq(TrainingRecord::getUserId, userId)
         .eq(TrainingRecord::getStatus, 1);
        long count = trainingRecordMapper.selectCount(w);
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private int countPerfectScore(Long userId) {
        LambdaQueryWrapper<TrainingRecord> w = new LambdaQueryWrapper<>();
        w.eq(TrainingRecord::getUserId, userId)
         .eq(TrainingRecord::getStatus, 1)
         .eq(TrainingRecord::getAccuracy, BigDecimal.valueOf(100));
        long count = trainingRecordMapper.selectCount(w);
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private int countAccuracy90(Long userId) {
        LambdaQueryWrapper<TrainingRecord> w = new LambdaQueryWrapper<>();
        w.eq(TrainingRecord::getUserId, userId)
         .eq(TrainingRecord::getStatus, 1)
         .ge(TrainingRecord::getAccuracy, BigDecimal.valueOf(90));
        long count = trainingRecordMapper.selectCount(w);
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private int getTotalStars(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user != null ? (user.getStarCount() != null ? user.getStarCount() : 0) : 0;
    }

    private int getCurrentStreak(Long userId) {
        LambdaQueryWrapper<UserStreak> w = new LambdaQueryWrapper<>();
        w.eq(UserStreak::getUserId, userId);
        UserStreak streak = userStreakMapper.selectOne(w);
        return streak != null && streak.getCurrentStreak() != null
                ? streak.getCurrentStreak() : 0;
    }
}
