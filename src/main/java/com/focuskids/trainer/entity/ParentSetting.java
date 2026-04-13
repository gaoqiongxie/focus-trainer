package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家长设置表
 */
@Data
@TableName("parent_setting")
public class ParentSetting implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long settingId;

    /** 用户ID(家长) */
    private Long userId;

    /** 孩子ID */
    private Long childId;

    /** 训练锁定模式(0:关闭 1:开启) */
    private Integer trainingLock;

    /** 每日训练时长限制(分钟) */
    private Integer dailyLimitMin;

    /** 每日提醒时间(HH:mm) */
    private String remindTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
