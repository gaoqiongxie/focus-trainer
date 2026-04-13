package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 训练会话表（断点续练）
 */
@Data
@TableName("training_session")
public class TrainingSession implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 训练记录ID */
    private Long recordId;

    /** 会话状态数据(JSON) */
    private String sessionData;

    /** 状态(0:已结束 1:进行中 2:已暂停) */
    private Integer status;

    /** 暂停次数 */
    private Integer pauseCount;

    /** 最后暂停时间 */
    private LocalDateTime lastPauseAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
