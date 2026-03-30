package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日任务表
 */
@Data
@TableName("daily_task")
public class DailyTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    /** 用户ID */
    private Long userId;

    /** 任务日期 */
    private LocalDate taskDate;

    /** 任务类型(1:完成训练 2:正确率达标 3:连续打卡 4:完成多种类型) */
    private Integer taskType;

    /** 任务标题 */
    private String title;

    /** 任务描述 */
    private String description;

    /** 目标值(如完成次数、正确率百分比) */
    private Integer targetValue;

    /** 当前进度值 */
    private Integer progressValue;

    /** 奖励星星数 */
    private Integer starReward;

    /** 任务状态(0:未完成 1:已完成 2:已领取奖励) */
    private Integer status;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 领取时间 */
    private LocalDateTime claimTime;

    private LocalDateTime createTime;
}
