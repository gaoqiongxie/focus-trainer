package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.DailyTask;
import com.focuskids.trainer.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 每日任务 Controller
 */
@RestController
@RequestMapping("/api/daily-task")
@RequiredArgsConstructor
public class DailyTaskController {

    private final DailyTaskService dailyTaskService;

    /**
     * 获取今日任务列表
     */
    @GetMapping("/today")
    public R<List<DailyTask>> getTodayTasks(@RequestHeader("X-User-Id") Long userId) {
        return R.success(dailyTaskService.getTodayTasks(userId));
    }

    /**
     * 获取指定日期的任务列表
     */
    @GetMapping("/date/{date}")
    public R<List<DailyTask>> getTasksByDate(@RequestHeader("X-User-Id") Long userId,
                                             @PathVariable String date) {
        LocalDate taskDate = LocalDate.parse(date);
        return R.success(dailyTaskService.getTasksByDate(userId, taskDate));
    }

    /**
     * 领取任务奖励
     */
    @PostMapping("/{taskId}/claim")
    public R<DailyTask> claimReward(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long taskId) {
        return R.success(dailyTaskService.claimReward(userId, taskId));
    }

    /**
     * 获取今日任务完成概况
     */
    @GetMapping("/summary")
    public R<Map<String, Object>> getTodaySummary(@RequestHeader("X-User-Id") Long userId) {
        return R.success(dailyTaskService.getTodaySummary(userId));
    }
}
