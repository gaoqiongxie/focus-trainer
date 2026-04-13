package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.DailyTask;
import com.focuskids.trainer.entity.TrainingRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 每日任务服务
 */
public interface DailyTaskService {

    /**
     * 获取今日任务列表（包含状态）
     */
    List<DailyTask> getTodayTasks(Long userId);

    /**
     * 获取指定日期的任务列表
     */
    List<DailyTask> getTasksByDate(Long userId, LocalDate date);

    /**
     * 完成任务并领取奖励
     */
    DailyTask claimReward(Long userId, Long taskId);

    /**
     * 获取今日任务完成概况
     */
    Map<String, Object> getTodaySummary(Long userId);

    /**
     * 训练完成后自动更新每日任务进度（由 TrainingServiceImpl 调用）
     */
    void onTrainingCompleteAfterCommit(TrainingRecord record);
}
