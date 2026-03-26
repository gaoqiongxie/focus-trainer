package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 激励记录表
 */
@Data
@TableName("reward_record")
public class RewardRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long rewardId;

    /** 用户ID */
    private Long userId;

    /** 奖励类型(1:星星 2:徽章 3:成就) */
    private Integer rewardType;

    /** 奖励值 */
    private Integer rewardValue;

    /** 奖励名称 */
    private String rewardName;

    /** 来源(1:训练完成 2:签到 3:活动) */
    private Integer sourceType;

    /** 来源ID */
    private Long sourceId;

    private LocalDateTime createTime;
}
