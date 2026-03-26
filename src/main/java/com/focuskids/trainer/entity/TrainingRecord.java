package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 训练记录表
 */
@Data
@TableName("training_record")
public class TrainingRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long recordId;

    /** 用户ID */
    private Long userId;

    /** 训练类型 */
    private Integer trainingType;

    /** 难度等级 */
    private Integer level;

    /** 计划时长(秒) */
    private Integer duration;

    /** 实际时长(秒) */
    private Integer actualDuration;

    /** 状态(0:进行中 1:完成 2:中断) */
    private Integer status;

    /** 中断次数 */
    private Integer interruptCount;

    /** 正确率(%) */
    private BigDecimal accuracy;

    /** 得分 */
    private Integer score;

    /** 获得星星数 */
    private Integer starReward;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 设备信息 */
    private String deviceInfo;

    private LocalDateTime createTime;
}
