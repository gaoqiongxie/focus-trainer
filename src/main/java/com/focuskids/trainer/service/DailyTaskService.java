package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.DailyTask;

import java.util.List;
import java.util.Map;

/**
 * 每日任务服务
 */
public interface DailyTaskService {

    /**
     * 获取用户今日任务列表（自动生成）
     */
    List<DailyTask> getTodayTasks(Long userId);

    /**
     * 获取用户指定日期的任务列表
     */
    List<DailyTask> getTasksByDate(Long userId, String date);

    /**
     * 完成训练后自动更新相关任务进度
     */
    void updateTaskProgress(Long userId, Integer trainingType, Integer accuracy, Integer status);

    /**
     * 领取任务奖励
     */
    DailyTask claimReward(Long userId, Long taskId);

    /**
     * 获取用户今日任务完成概况
     */
    Map<String, Object> getTodaySummary(Long userId);
}
