package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户徽章领取记录
 */
@Data
@TableName("user_badge")
public class UserBadge implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 徽章ID */
    private Integer badgeId;

    /** 获得时间 */
    private LocalDateTime earnedAt;

    /** 来源: 1=训练 2=打卡 3=购买 4=系统 */
    private Integer sourceType;

    /** 来源ID（训练记录ID等） */
    private Long sourceId;
}
