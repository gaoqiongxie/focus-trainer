package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息表
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long notificationId;

    /** 用户ID */
    private Long userId;

    /** 类型(1:训练提醒 2:奖励通知 3:专家建议) */
    private Integer type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 是否已读(0:未读 1:已读) */
    private Integer isRead;

    /** 扩展数据(JSON) */
    private String extraData;

    private LocalDateTime createTime;
}
