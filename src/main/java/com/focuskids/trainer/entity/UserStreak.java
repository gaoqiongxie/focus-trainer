package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户连续打卡表
 */
@Data
@TableName("user_streak")
public class UserStreak implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long streakId;

    /** 用户ID */
    private Long userId;

    /** 当前连续天数 */
    private Integer currentStreak;

    /** 历史最大连续天数 */
    private Integer maxStreak;

    /** 最后训练日期 */
    private LocalDate lastTrainDate;

    private LocalDateTime updateTime;
}
