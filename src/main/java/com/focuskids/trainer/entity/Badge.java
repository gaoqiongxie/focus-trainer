package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 徽章定义表（系统预置）
 */
@Data
@TableName("badge")
public class Badge implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer badgeId;

    /** 徽章唯一标识 */
    private String badgeKey;

    /** 徽章名称 */
    private String name;

    /** 徽章描述 */
    private String description;

    /** 图标emoji */
    private String icon;

    /** 类别: streak/completion/accuracy/stars/special */
    private String category;

    /** 条件类型 */
    private String conditionType;

    /** 条件阈值 */
    private Integer conditionValue;

    /** 解锁所需星星 */
    private Integer starCost;

    /** 是否启用 */
    private Integer isActive;

    /** 展示顺序 */
    private Integer displayOrder;

    private LocalDateTime createTime;
}
