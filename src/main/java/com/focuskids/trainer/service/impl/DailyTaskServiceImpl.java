package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.entity.DailyTask;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.entity.UserStreak;
import com.focuskids.trainer.mapper.DailyTaskMapper;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.mapper.UserStreakMapper;
import com.focuskids.trainer.service.DailyTaskService;
import com.focuskids.trainer.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 每日任务服务实现
 */
@Service
@RequiredArgsConstructor
public class DailyTaskServiceImpl implements DailyTaskService {

    private final DailyTaskMapper dailyTaskMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final UserStreakMapper userStreakMapper;
    private final RewardService rewardService;
    private final StringRedisTemplate redisTemplate;

    /** 任务类型定义 */
    private static final int TASK_COMPLETE_TRAINING = 1;    // 完成N次训练
    private static final int TASK_ACCURACY_TARGET = 2;       // 单次正确率达标
    private static final int TASK_STREAK_CHECK = 3;          // 连续打卡
    private static final int TASK_MULTI_TYPE = 4;            // 完成多种类型训练

    /** 默认任务模板 */
    private static final List<TaskTemplate> TASK_TEMPLATES = Arrays.asList(
            new TaskTemplate(TASK_COMPLETE_TRAINING, "训练达人", "完成3次训练", 3, 5),
            new TaskTemplate(TASK_ACCURACY_TARGET, "精准打击", "单次训练正确率达到80%", 80, 3),
            new TaskTemplate(TASK_STREAK_CHECK, "持之以恒", "连续打卡（自动完成）", 1, 2),
            new TaskTemplate(TASK_MULTI_TYPE, "全面发展", "完成2种不同类型训练", 2, 5)
    );

    @Override
    public List<DailyTask> getTodayTasks(Long userId) {
        return getOrCreateTasks(userId, LocalDate.now());
    }

    @Override
    public List<DailyTask> getTasksByDate(Long userId, String date) {
        LocalDate taskDate = LocalDate.parse(date);
        return getOrCreateTasks(userId, taskDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskProgress(Long userId, Integer trainingType, Integer accuracy, Integer status) {
        LocalDate today = LocalDate.now();
        List<DailyTask> tasks = dailyTaskMapper.selectByUserIdAndDate(userId, today);
        if (tasks.isEmpty()) {
            return;
        }

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        // 查询今日所有已完成训练记录
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
               .eq(TrainingRecord::getStatus, 1)
               .ge(TrainingRecord::getStartTime, dayStart)
               .le(TrainingRecord::getStartTime, dayEnd);
        List<TrainingRecord> completedRecords = trainingRecordMapper.selectList(wrapper);

        // 收集今日已完成的训练类型
        Set<Integer> completedTypes = new HashSet<>();
        for (TrainingRecord r : completedRecords) {
            completedTypes.add(r.getTrainingType());
        }

        for (DailyTask task : tasks) {
            if (task.getStatus() != 0) continue; // 跳过已完成或已领取

            boolean completed = false;
            int progress = 0;

            switch (task.getTaskType()) {
                case TASK_COMPLETE_TRAINING:
                    progress = completedRecords.size();
                    completed = progress >= task.getTargetValue();
                    break;

                case TASK_ACCURACY_TARGET:
                    // 检查是否有任何一条记录达到目标正确率
                    for (TrainingRecord r : completedRecords) {
                        if (r.getAccuracy() != null && r.getAccuracy().doubleValue() >= task.getTargetValue()) {
                            completed = true;
                            // 记录最高正确率作为进度
                            int acc = r.getAccuracy().intValue();
                            if (acc > progress) progress = acc;
                        }
                    }
                    if (progress == 0) progress = 0;
                    break;

                case TASK_STREAK_CHECK:
                    // 检查连续打卡
                    UserStreak streak = userStreakMapper.selectOne(
                            new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, userId));
                    if (streak != null && streak.getLastTrainDate() != null
                            && (streak.getLastTrainDate().isEqual(today) || streak.getLastTrainDate().plusDays(1).isEqual(today))) {
                        completed = true;
                        progress = streak.getCurrentStreak();
                    }
                    break;

                case TASK_MULTI_TYPE:
                    // 需要将子类型映射到大类
                    Set<Integer> categories = new HashSet<>();
                    for (Integer type : completedTypes) {
                        categories.add(mapTypeToCategory(type));
                    }
                    progress = categories.size();
                    completed = progress >= task.getTargetValue();
                    break;

                default:
                    break;
            }

            task.setProgressValue(progress);
            if (completed) {
                task.setStatus(1);
                task.setCompleteTime(LocalDateTime.now());
            }
            dailyTaskMapper.updateById(task);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyTask claimReward(Long userId, Long taskId) {
        DailyTask task = dailyTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 校验权属
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 只能领取已完成的任务
        if (task.getStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED);
        }

        // 标记为已领取
        task.setStatus(2);
        task.setClaimTime(LocalDateTime.now());
        dailyTaskMapper.updateById(task);

        // 发放星星奖励
        if (task.getStarReward() != null && task.getStarReward() > 0) {
            rewardService.addStars(userId, task.getStarReward(), 3, taskId);
        }

        return task;
    }

    @Override
    public Map<String, Object> getTodaySummary(Long userId) {
        List<DailyTask> tasks = getTodayTasks(userId);

        int total = tasks.size();
        int completed = 0;
        int claimed = 0;
        int totalStars = 0;

        for (DailyTask task : tasks) {
            if (task.getStatus() == 1) completed++;
            if (task.getStatus() == 2) {
                claimed++;
                totalStars += task.getStarReward() != null ? task.getStarReward() : 0;
            }
            totalStars += task.getStatus() == 1 && task.getStarReward() != null ? task.getStarReward() : 0;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("completed", completed);
        summary.put("claimed", claimed);
        summary.put("pendingReward", completed - claimed);
        summary.put("totalStars", totalStars);
        summary.put("tasks", tasks);
        return summary;
    }

    // ==================== 私有方法 ====================

    private List<DailyTask> getOrCreateTasks(Long userId, LocalDate taskDate) {
        List<DailyTask> tasks = dailyTaskMapper.selectByUserIdAndDate(userId, taskDate);
        if (!tasks.isEmpty()) {
            return tasks;
        }

        // 只能为今天或过去生成任务（不能为未来生成）
        if (taskDate.isAfter(LocalDate.now())) {
            return Collections.emptyList();
        }

        // 生成默认任务
        List<DailyTask> newTasks = new ArrayList<>();
        for (TaskTemplate template : TASK_TEMPLATES) {
            DailyTask task = new DailyTask();
            task.setUserId(userId);
            task.setTaskDate(taskDate);
            task.setTaskType(template.taskType);
            task.setTitle(template.title);
            task.setDescription(template.description);
            task.setTargetValue(template.targetValue);
            task.setProgressValue(0);
            task.setStarReward(template.starReward);
            task.setStatus(0);
            dailyTaskMapper.insert(task);
            newTasks.add(task);
        }

        // 如果不是今天，回填历史进度
        if (!taskDate.isEqual(LocalDate.now())) {
            backfillProgress(newTasks, userId, taskDate);
        }

        return newTasks;
    }

    private void backfillProgress(List<DailyTask> tasks, Long userId, LocalDate taskDate) {
        LocalDateTime dayStart = taskDate.atStartOfDay();
        LocalDateTime dayEnd = taskDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
               .eq(TrainingRecord::getStatus, 1)
               .ge(TrainingRecord::getStartTime, dayStart)
               .le(TrainingRecord::getStartTime, dayEnd);
        List<TrainingRecord> records = trainingRecordMapper.selectList(wrapper);

        Set<Integer> types = new HashSet<>();
        for (TrainingRecord r : records) {
            types.add(r.getTrainingType());
        }

        for (DailyTask task : tasks) {
            boolean completed = false;
            int progress = 0;

            switch (task.getTaskType()) {
                case TASK_COMPLETE_TRAINING:
                    progress = records.size();
                    completed = progress >= task.getTargetValue();
                    break;
                case TASK_ACCURACY_TARGET:
                    for (TrainingRecord r : records) {
                        if (r.getAccuracy() != null && r.getAccuracy().doubleValue() >= task.getTargetValue()) {
                            completed = true;
                        }
                    }
                    break;
                case TASK_STREAK_CHECK:
                    UserStreak streak = userStreakMapper.selectOne(
                            new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, userId));
                    if (streak != null && streak.getLastTrainDate() != null
                            && !streak.getLastTrainDate().isAfter(taskDate)) {
                        completed = true;
                    }
                    break;
                case TASK_MULTI_TYPE:
                    Set<Integer> categories = new HashSet<>();
                    for (Integer type : types) {
                        categories.add(mapTypeToCategory(type));
                    }
                    progress = categories.size();
                    completed = progress >= task.getTargetValue();
                    break;
            }

            task.setProgressValue(progress);
            if (completed) {
                task.setStatus(1);
                task.setCompleteTime(dayEnd);
            }
            dailyTaskMapper.updateById(task);
        }
    }

    /**
     * 将训练子类型映射到大类
     * 1→1(专注时长), 2→2(视觉追踪), 21→2(视觉追踪), 3→3(听觉专注), 4→4(记忆训练), 41→4(记忆训练)
     */
    private int mapTypeToCategory(Integer trainingType) {
        if (trainingType == null) return 0;
        switch (trainingType) {
            case 1: return 1;
            case 2:
            case 21: return 2;
            case 3: return 3;
            case 4:
            case 41: return 4;
            default: return 0;
        }
    }

    private static class TaskTemplate {
        int taskType;
        String title;
        String description;
        int targetValue;
        int starReward;

        TaskTemplate(int taskType, String title, String description, int targetValue, int starReward) {
            this.taskType = taskType;
            this.title = title;
            this.description = description;
            this.targetValue = targetValue;
            this.starReward = starReward;
        }
    }
}
