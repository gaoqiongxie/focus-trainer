package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.entity.Badge;
import com.focuskids.trainer.entity.UserBadge;
import com.focuskids.trainer.mapper.BadgeMapper;
import com.focuskids.trainer.mapper.UserBadgeMapper;
import com.focuskids.trainer.service.BadgeRuleEngine;
import com.focuskids.trainer.service.BadgeService;
import com.focuskids.trainer.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final BadgeMapper        badgeMapper;
    private final UserBadgeMapper    userBadgeMapper;
    private final BadgeRuleEngine   badgeRuleEngine;
    private final NotificationService notificationService;

    @Override
    public List<Map<String, Object>> getUserBadges(Long userId) {
        // 所有活跃徽章
        List<Badge> allBadges = badgeMapper.selectAllActive();
        if (allBadges.isEmpty()) return Collections.emptyList();

        // 用户已获得的徽章ID集合
        Set<Integer> earnedSet = new HashSet<>(userBadgeMapper.selectBadgeIdsByUserId(userId));

        // 构建返回列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (Badge badge : allBadges) {
            boolean earned = earnedSet.contains(badge.getBadgeId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("badgeId",    badge.getBadgeId());
            map.put("badgeKey",   badge.getBadgeKey());
            map.put("name",       badge.getName());
            map.put("description",badge.getDescription());
            map.put("icon",       badge.getIcon());
            map.put("category",   badge.getCategory());
            map.put("earned",     earned);
            map.put("earnedAt",   null); // 已获得的徽章可补充时间，这里简化处理
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Badge> checkAndUnlockAfterTraining(Long userId, Long trainingRecordId) {
        List<Badge> newBadges = badgeRuleEngine.evaluateAfterTraining(userId, trainingRecordId);

        // 发送通知
        for (Badge badge : newBadges) {
            notificationService.send(
                    userId,
                    2, // 奖励通知
                    "🏅 " + badge.getName() + " 解锁！",
                    "恭喜你解锁了「" + badge.getName() + "」！" + badge.getDescription()
            );
        }

        return newBadges;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Badge> checkAndUnlockAfterStreak(Long userId, int streakDays) {
        List<Badge> newBadges = badgeRuleEngine.evaluateAfterStreak(userId, streakDays);

        for (Badge badge : newBadges) {
            notificationService.send(
                    userId,
                    2,
                    "🏅 " + badge.getName() + " 解锁！",
                    "恭喜你解锁了「" + badge.getName() + "」！" + badge.getDescription()
            );
        }

        return newBadges;
    }

    @Override
    public int getEarnedCount(Long userId) {
        return userBadgeMapper.countByUserId(userId);
    }
}
