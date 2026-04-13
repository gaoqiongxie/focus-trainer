package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focuskids.trainer.entity.RewardRecord;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.entity.UserStreak;
import com.focuskids.trainer.mapper.RewardRecordMapper;
import com.focuskids.trainer.mapper.SysUserMapper;
import com.focuskids.trainer.mapper.UserStreakMapper;
import com.focuskids.trainer.service.BadgeService;
import com.focuskids.trainer.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final SysUserMapper userMapper;
    private final RewardRecordMapper rewardMapper;
    private final UserStreakMapper streakMapper;
    private final StringRedisTemplate redisTemplate;
    private final BadgeService badgeService;

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
        // 委托给 BadgeService 返回完整徽章数据
        return badgeService.getUserBadges(userId);
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
    @Transactional(rollbackFor = Exception.class)
    public UserStreak updateStreak(Long userId) {
        // 使用 Redis 分布式锁防止并发竞态
        String lockKey = "streak:lock:" + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            // 未获取到锁，返回当前记录
            return getStreak(userId);
        }

        try {
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
                // 连续打卡 — 使用数据库原子更新避免竞态
                streakMapper.update(null, new LambdaUpdateWrapper<UserStreak>()
                        .eq(UserStreak::getUserId, userId)
                        .eq(UserStreak::getLastTrainDate, streak.getLastTrainDate())
                        .setSql("current_streak = current_streak + 1, max_streak = GREATEST(max_streak, current_streak + 1), last_train_date = '" + today + "'"));

                // 刷新对象
                streak = streakMapper.selectOne(wrapper);
            } else if (streak.getLastTrainDate().isEqual(today)) {
                // 今天已打卡，不处理
            } else {
                // 中断了，重新开始
                streakMapper.update(null, new LambdaUpdateWrapper<UserStreak>()
                        .eq(UserStreak::getUserId, userId)
                        .setSql("current_streak = 1, last_train_date = '" + today + "'"));
                streak = streakMapper.selectOne(wrapper);
            }

            // 检查是否获得连续打卡徽章
            checkStreakBadge(userId, streak.getCurrentStreak());

            return streak;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void checkStreakBadge(Long userId, int streakDays) {
        // 委托给 BadgeService 统一处理打卡徽章解锁
        try {
            badgeService.checkAndUnlockAfterStreak(userId, streakDays);
        } catch (Exception e) {
            // 打卡徽章解锁失败不影响打卡流程
        }
    }
}
