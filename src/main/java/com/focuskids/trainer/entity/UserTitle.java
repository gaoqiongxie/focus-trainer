package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 称号定义表（系统预置）
 */
@Data
@TableName("user_title")
public class UserTitle implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer titleId;

    /** 称号唯一标识 */
    private String titleKey;

    /** 称号名称 */
    private String name;

    /** 称号描述 */
    private String description;

    /** 图标emoji */
    private String icon;

    /** 类别: level/special/achievement */
    private String category;

    /** 解锁条件类型 */
    private String unlockType;

    /** 解锁条件阈值 */
    private Integer unlockValue;

    /** 展示顺序 */
    private Integer displayOrder;

    /** 是否启用 */
    private Integer isActive;

    private LocalDateTime createTime;
}
