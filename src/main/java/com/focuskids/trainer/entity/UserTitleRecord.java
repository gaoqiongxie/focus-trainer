package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户称号记录表
 */
@Data
@TableName("user_title_record")
public class UserTitleRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 称号ID */
    private Integer titleId;

    /** 是否装备中(0:否 1:是) */
    private Integer isEquipped;

    /** 解锁时间 */
    private LocalDateTime unlockedAt;
}
