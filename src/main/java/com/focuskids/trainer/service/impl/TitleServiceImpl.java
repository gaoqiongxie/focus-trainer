package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.entity.*;
import com.focuskids.trainer.mapper.*;
import com.focuskids.trainer.service.TitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TitleServiceImpl implements TitleService {

    private final UserTitleMapper userTitleMapper;
    private final UserTitleRecordMapper userTitleRecordMapper;
    private final UserAbilityMapper userAbilityMapper;
    private final SysUserMapper sysUserMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final UserStreakMapper userStreakMapper;

    @Override
    public List<UserTitle> listTitles() {
        return userTitleMapper.selectList(
                new LambdaQueryWrapper<UserTitle>()
                        .eq(UserTitle::getIsActive, 1)
                        .orderByAsc(UserTitle::getCategory)
                        .orderByAsc(UserTitle::getDisplayOrder)
        );
    }

    @Override
    public Map<String, Object> getCurrentTitle(Long userId) {
        UserTitleRecord equipped = userTitleRecordMapper.selectOne(
                new LambdaQueryWrapper<UserTitleRecord>()
                        .eq(UserTitleRecord::getUserId, userId)
                        .eq(UserTitleRecord::getIsEquipped, 1)
        );

        Map<String, Object> result = new HashMap<>();
        if (equipped != null) {
            UserTitle title = userTitleMapper.selectById(equipped.getTitleId());
            if (title != null) {
                result.put("titleId", title.getTitleId());
                result.put("titleKey", title.getTitleKey());
                result.put("name", title.getName());
                result.put("icon", title.getIcon());
                result.put("description", title.getDescription());
                result.put("category", title.getCategory());
                result.put("equippedAt", equipped.getUnlockedAt());
            }
        } else {
            result.put("titleId", null);
            result.put("name", "初学者");
            result.put("icon", "🌱");
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getUnlockedTitles(Long userId) {
        List<UserTitleRecord> records = userTitleRecordMapper.selectList(
                new LambdaQueryWrapper<UserTitleRecord>()
                        .eq(UserTitleRecord::getUserId, userId)
                        .orderByDesc(UserTitleRecord::getUnlockedAt)
        );

        return records.stream().map(r -> {
            UserTitle title = userTitleMapper.selectById(r.getTitleId());
            Map<String, Object> item = new HashMap<>();
            if (title != null) {
                item.put("titleId", title.getTitleId());
                item.put("titleKey", title.getTitleKey());
                item.put("name", title.getName());
                item.put("icon", title.getIcon());
                item.put("description", title.getDescription());
                item.put("category", title.getCategory());
            }
            item.put("isEquipped", r.getIsEquipped());
            item.put("unlockedAt", r.getUnlockedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void equipTitle(Long userId, Integer titleId) {
        // 检查称号是否已解锁
        UserTitleRecord record = userTitleRecordMapper.selectOne(
                new LambdaQueryWrapper<UserTitleRecord>()
                        .eq(UserTitleRecord::getUserId, userId)
                        .eq(UserTitleRecord::getTitleId, titleId)
        );
        if (record == null) {
            throw new BusinessException("称号未解锁");
        }

        // 先取消当前装备
        userTitleRecordMapper.update(null,
                new LambdaUpdateWrapper<UserTitleRecord>()
                        .eq(UserTitleRecord::getUserId, userId)
                        .eq(UserTitleRecord::getIsEquipped, 1)
                        .set(UserTitleRecord::getIsEquipped, 0)
        );

        // 装备新称号
        record.setIsEquipped(1);
        userTitleRecordMapper.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<UserTitle> checkAndUnlockTitles(Long userId) {
        // 获取用户已解锁的称号ID
        Set<Integer> unlockedIds = userTitleRecordMapper.selectList(
                new LambdaQueryWrapper<UserTitleRecord>().eq(UserTitleRecord::getUserId, userId)
        ).stream().map(UserTitleRecord::getTitleId).collect(Collectors.toSet());

        // 获取所有可用称号
        List<UserTitle> allTitles = userTitleMapper.selectList(
                new LambdaQueryWrapper<UserTitle>().eq(UserTitle::getIsActive, 1)
        );

        List<UserTitle> newlyUnlocked = new ArrayList<>();

        for (UserTitle title : allTitles) {
            if (unlockedIds.contains(title.getTitleId())) {
                continue;
            }

            boolean shouldUnlock = evaluateUnlockCondition(userId, title);
            if (shouldUnlock) {
                UserTitleRecord record = new UserTitleRecord();
                record.setUserId(userId);
                record.setTitleId(title.getTitleId());
                record.setIsEquipped(0);
                userTitleRecordMapper.insert(record);
                newlyUnlocked.add(title);
                log.info("用户{}解锁称号: {}", userId, title.getName());
            }
        }

        return newlyUnlocked;
    }

    private boolean evaluateUnlockCondition(Long userId, UserTitle title) {
        String unlockType = title.getUnlockType();
        int unlockValue = title.getUnlockValue();

        switch (unlockType) {
            case "total_score": {
                UserAbility ability = getLatestAbility(userId);
                if (ability == null) return false;
                int total = ability.getTotalScore() != null ? ability.getTotalScore().intValue() : 0;
                return total >= unlockValue;
            }
            case "visual_score": {
                UserAbility ability = getLatestAbility(userId);
                if (ability == null) return false;
                int score = ability.getVisualAttention() != null ? ability.getVisualAttention().intValue() : 0;
                return score >= unlockValue;
            }
            case "memory_score": {
                UserAbility ability = getLatestAbility(userId);
                if (ability == null) return false;
                int score = ability.getWorkingMemory() != null ? ability.getWorkingMemory().intValue() : 0;
                return score >= unlockValue;
            }
            case "focus_score": {
                UserAbility ability = getLatestAbility(userId);
                if (ability == null) return false;
                int score = ability.getAttentionDuration() != null ? ability.getAttentionDuration().intValue() : 0;
                return score >= unlockValue;
            }
            case "auditory_score": {
                UserAbility ability = getLatestAbility(userId);
                if (ability == null) return false;
                int score = ability.getAuditoryAttention() != null ? ability.getAuditoryAttention().intValue() : 0;
                return score >= unlockValue;
            }
            case "control_score": {
                UserAbility ability = getLatestAbility(userId);
                if (ability == null) return false;
                int score = ability.getInhibitoryControl() != null ? ability.getInhibitoryControl().intValue() : 0;
                return score >= unlockValue;
            }
            case "all_dimensions": {
                UserAbility ability = getLatestAbility(userId);
                if (ability == null) return false;
                int min = getMinDimensionScore(ability);
                return min >= unlockValue;
            }
            case "streak_days": {
                UserStreak streak = getStreak(userId);
                int days = streak != null ? streak.getCurrentStreak() : 0;
                return days >= unlockValue;
            }
            case "training_count": {
                long count = trainingRecordMapper.selectCount(
                        new LambdaQueryWrapper<TrainingRecord>()
                                .eq(TrainingRecord::getUserId, userId)
                                .eq(TrainingRecord::getStatus, 1)
                );
                return count >= unlockValue;
            }
            case "total_stars": {
                SysUser user = sysUserMapper.selectById(userId);
                int stars = user != null && user.getStarCount() != null ? user.getStarCount() : 0;
                return stars >= unlockValue;
            }
            default:
                return false;
        }
    }

    private UserAbility getLatestAbility(Long userId) {
        return userAbilityMapper.selectOne(
                new LambdaQueryWrapper<UserAbility>()
                        .eq(UserAbility::getUserId, userId)
                        .orderByDesc(UserAbility::getEvaluateDate)
                        .last("LIMIT 1")
        );
    }

    private UserStreak getStreak(Long userId) {
        return userStreakMapper.selectOne(
                new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, userId)
        );
    }

    private int getMinDimensionScore(UserAbility ability) {
        int min = Integer.MAX_VALUE;
        if (ability.getAttentionDuration() != null) min = Math.min(min, ability.getAttentionDuration().intValue());
        if (ability.getVisualAttention() != null) min = Math.min(min, ability.getVisualAttention().intValue());
        if (ability.getAuditoryAttention() != null) min = Math.min(min, ability.getAuditoryAttention().intValue());
        if (ability.getWorkingMemory() != null) min = Math.min(min, ability.getWorkingMemory().intValue());
        if (ability.getInhibitoryControl() != null) min = Math.min(min, ability.getInhibitoryControl().intValue());
        return min == Integer.MAX_VALUE ? 0 : min;
    }
}
