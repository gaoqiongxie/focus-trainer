package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focuskids.trainer.entity.RewardRecord;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.entity.UserStreak;
import com.focuskids.trainer.mapper.RewardRecordMapper;
import com.focuskids.trainer.mapper.SysUserMapper;
import com.focuskids.trainer.mapper.UserStreakMapper;
import com.focuskids.trainer.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final SysUserMapper userMapper;
    private final RewardRecordMapper rewardMapper;
    private final UserStreakMapper streakMapper;

    private static final List<String> BADGE_NAMES = Arrays.asList(
            "初出茅庐", "坚持不懈", "训练达人", "专注之星",
            "视觉猎手", "听觉大师", "记忆高手", "连续7天",
            "连续14天", "连续30天", "满分通关", "百星少年"
    );

    @Override
    public int getStarCount(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user != null ? user.getStarCount() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStars(Long userId, int count, Integer sourceType, Long sourceId) {
        // 使用原子更新避免并发竞态（SQL: UPDATE SET star_count = star_count + count）
        userMapper.addStars(userId, count);

        // 记录星星奖励
        RewardRecord record = new RewardRecord();
        record.setUserId(userId);
        record.setRewardType(1);
        record.setRewardValue(count);
        record.setRewardName(count + "颗星星");
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        rewardMapper.insert(record);
    }

    @Override
    public List<Map<String, Object>> getBadges(Long userId) {
        LambdaQueryWrapper<RewardRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardRecord::getUserId, userId)
               .eq(RewardRecord::getRewardType, 2)
               .orderByDesc(RewardRecord::getCreateTime);

        List<RewardRecord> records = rewardMapper.selectList(wrapper);
        Set<String> earnedBadgeNames = new HashSet<>();
        for (RewardRecord r : records) {
            earnedBadgeNames.add(r.getRewardName());
        }

        // 构建徽章列表（含已获得和未获得）
        List<Map<String, Object>> badges = new ArrayList<>();
        for (int i = 0; i < BADGE_NAMES.size(); i++) {
            Map<String, Object> badge = new HashMap<>();
            badge.put("id", i + 1);
            badge.put("name", BADGE_NAMES.get(i));
            badge.put("earned", earnedBadgeNames.contains(BADGE_NAMES.get(i)));
            badges.add(badge);
        }
        return badges;
    }

    @Override
    public UserStreak getStreak(Long userId) {
        LambdaQueryWrapper<UserStreak> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStreak::getUserId, userId);
        UserStreak streak = streakMapper.selectOne(wrapper);
        if (streak == null) {
            streak = new UserStreak();
            streak.setUserId(userId);
            streak.setCurrentStreak(0);
            streak.setMaxStreak(0);
        }
        return streak;
    }

    @Override
    @Transactional
    public UserStreak updateStreak(Long userId) {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<UserStreak> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStreak::getUserId, userId);
        UserStreak streak = streakMapper.selectOne(wrapper);

        if (streak == null) {
            streak = new UserStreak();
            streak.setUserId(userId);
            streak.setCurrentStreak(1);
            streak.setMaxStreak(1);
            streak.setLastTrainDate(today);
            streakMapper.insert(streak);
        } else if (streak.getLastTrainDate() == null || streak.getLastTrainDate().plusDays(1).isEqual(today)) {
            // 连续打卡
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            if (streak.getCurrentStreak() > streak.getMaxStreak()) {
                streak.setMaxStreak(streak.getCurrentStreak());
            }
            streak.setLastTrainDate(today);
            streakMapper.updateById(streak);
        } else if (streak.getLastTrainDate().isEqual(today)) {
            // 今天已打卡，不处理
        } else {
            // 中断了，重新开始
            streak.setCurrentStreak(1);
            streak.setLastTrainDate(today);
            streakMapper.updateById(streak);
        }

        // 检查是否获得连续打卡徽章
        checkStreakBadge(userId, streak.getCurrentStreak());

        return streak;
    }

    private void checkStreakBadge(Long userId, int streakDays) {
        Map<Integer, String> badgeMap = new HashMap<>();
        badgeMap.put(7, "连续7天");
        badgeMap.put(14, "连续14天");
        badgeMap.put(30, "连续30天");

        if (badgeMap.containsKey(streakDays)) {
            RewardRecord record = new RewardRecord();
            record.setUserId(userId);
            record.setRewardType(2);
            record.setRewardValue(streakDays);
            record.setRewardName(badgeMap.get(streakDays));
            record.setSourceType(2);
            rewardMapper.insert(record);
        }
    }
}
