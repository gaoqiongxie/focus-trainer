package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 训练配置表
 */
@Data
@TableName("training_config")
public class TrainingConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer configId;

    /** 训练类型(1:专注时长 2:舒尔特方格 3:听觉专注 4:卡片配对 21:数字闪现) */
    private Integer trainingType;

    /** 训练大类(1:专注时长 2:视觉追踪 3:听觉专注 4:记忆训练) */
    private Integer category;

    /** 训练名称 */
    private String trainingName;

    /** 难度等级(1-10) */
    private Integer level;

    /** 训练时长(秒) */
    private Integer duration;

    /** 详细配置(JSON) */
    private String configJson;

    /** 训练图标 */
    private String iconUrl;

    /** 训练描述 */
    private String description;

    /** 是否启用(0:否 1:是) */
    private Integer isActive;

    /** 训练大类(1:专注时长 2:视觉追踪 3:听觉专注 4:记忆训练) */
    private Integer category;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
