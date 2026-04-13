package com.focuskids.trainer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.entity.DailyTask;
import com.focuskids.trainer.entity.RewardRecord;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.mapper.DailyTaskMapper;
import com.focuskids.trainer.mapper.RewardRecordMapper;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyTaskServiceImpl implements DailyTaskService {

    private final DailyTaskMapper dailyTaskMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final RewardRecordMapper rewardRecordMapper;

    @Override
    public List<DailyTask> getTodayTasks(Long userId) {
        return dailyTaskMapper.selectByUserIdAndDate(userId, LocalDate.now());
    }

    @Override
    public List<DailyTask> getTasksByDate(Long userId, LocalDate date) {
        return dailyTaskMapper.selectByUserIdAndDate(userId, date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyTask claimReward(Long userId, Long taskId) {
        DailyTask task = dailyTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此任务");
        }
        if (task.getStatus() == 2) {
            throw new RuntimeException("奖励已领取");
        }
        if (task.getStatus() == 0) {
            throw new RuntimeException("任务未完成，无法领取");
        }

        // 标记为已领取
        task.setStatus(2);
        task.setClaimTime(LocalDateTime.now());
        dailyTaskMapper.updateById(task);

        // 发放星星奖励
        if (task.getStarReward() != null && task.getStarReward() > 0) {
            RewardRecord reward = new RewardRecord();
            reward.setUserId(userId);
            reward.setRewardType(1); // 星星
            reward.setRewardValue(task.getStarReward());
            reward.setRewardName("每日任务奖励");
            reward.setSourceType(1); // 训练完成
            reward.setSourceId(taskId);
            reward.setCreateTime(LocalDateTime.now());
            rewardRecordMapper.insert(reward);
            log.info("用户 {} 领取每日任务 {} 奖励: {} 星星", userId, taskId, task.getStarReward());
        }

        return dailyTaskMapper.selectById(taskId);
    }

    @Override
    public Map<String, Object> getTodaySummary(Long userId) {
        List<DailyTask> tasks = getTodayTasks(userId);
        Map<String, Object> summary = new HashMap<>();
        summary.put("total", tasks.size());
        summary.put("completed", (int) tasks.stream().filter(t -> t.getStatus() >= 1).count());
        summary.put("claimed", (int) tasks.stream().filter(t -> t.getStatus() == 2).count());
        summary.put("unclaimed", (int) tasks.stream().filter(t -> t.getStatus() == 1).count());
        summary.put("totalStars", tasks.stream()
                .filter(t -> t.getStatus() == 2)
                .mapToInt(t -> t.getStarReward() == null ? 0 : t.getStarReward())
                .sum());
        summary.put("completionRate", tasks.isEmpty() ? 0 :
                (int) ((double) tasks.stream().filter(t -> t.getStatus() >= 1).count() / tasks.size() * 100));
        return summary;
    }

    /**
     * 训练完成后更新每日任务进度（由 TrainingServiceImpl 训练结束后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void onTrainingCompleteAfterCommit(TrainingRecord record) {
        if (record.getStatus() != 1) return; // 只处理完成的训练

        Long userId = record.getUserId();
        LocalDate today = LocalDate.now();
        List<DailyTask> tasks = dailyTaskMapper.selectByUserIdAndDate(userId, today);
        if (tasks.isEmpty()) return;

        for (DailyTask task : tasks) {
            boolean shouldUpdate = false;

            switch (task.getTaskType()) {
                case 1: // 完成训练：任何类型完成1次即达成
                    if (task.getStatus() == 0) {
                        task.setProgressValue(1);
                        task.setStatus(1);
                        shouldUpdate = true;
                    }
                    break;

                case 2: // 正确率达标：今日所有训练平均正确率达到 target_value
                    BigDecimal todayAccuracy = getTodayAvgAccuracy(userId);
                    if (todayAccuracy != null && todayAccuracy.compareTo(BigDecimal.valueOf(task.getTargetValue())) >= 0) {
                        if (task.getStatus() == 0) {
                            task.setProgressValue(todayAccuracy.intValue());
                            task.setStatus(1);
                            shouldUpdate = true;
                        }
                    } else if (record.getAccuracy() != null) {
                        task.setProgressValue(record.getAccuracy().intValue());
                        shouldUpdate = true;
                    }
                    break;

                case 3: // 连续打卡：由打卡服务处理，这里检查 streak 更新
                    // 跳过，由打卡逻辑处理
                    break;

                case 4: // 多维训练：今日完成的不同类型数量 >= target_value
                    if (task.getStatus() == 0) {
                        int distinctTypes = countDistinctTrainingTypesToday(userId);
                        task.setProgressValue(distinctTypes);
                        if (distinctTypes >= task.getTargetValue()) {
                            task.setStatus(1);
                        }
                        shouldUpdate = true;
                    }
                    break;
            }

            if (shouldUpdate) {
                dailyTaskMapper.updateById(task);
                log.info("更新每日任务进度: taskId={}, progress={}, status={}",
                        task.getTaskId(), task.getProgressValue(), task.getStatus());
            }
        }
    }

    private BigDecimal getTodayAvgAccuracy(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        Map<String, Object> stats = trainingRecordMapper.selectTrainingStatistics(userId, start, end);
        if (stats == null) return null;
        Object avg = stats.get("avgAccuracy");
        if (avg == null) return null;
        return new BigDecimal(avg.toString());
    }

    private int countDistinctTrainingTypesToday(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
                .eq(TrainingRecord::getStatus, 1)
                .ge(TrainingRecord::getStartTime, start)
                .lt(TrainingRecord::getStartTime, end)
                .select(TrainingRecord::getTrainingType);
        List<TrainingRecord> records = trainingRecordMapper.selectList(wrapper);
        return (int) records.stream().map(TrainingRecord::getTrainingType).distinct().count();
    }
}
