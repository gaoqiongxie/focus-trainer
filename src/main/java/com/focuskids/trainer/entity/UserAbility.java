package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户能力评估表
 */
@Data
@TableName("user_ability")
public class UserAbility implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long abilityId;

    /** 用户ID */
    private Long userId;

    /** 专注时长得分 */
    private BigDecimal attentionDuration;

    /** 视觉注意力得分 */
    private BigDecimal visualAttention;

    /** 听觉注意力得分 */
    private BigDecimal auditoryAttention;

    /** 工作记忆得分 */
    private BigDecimal workingMemory;

    /** 抑制控制得分 */
    private BigDecimal inhibitoryControl;

    /** 综合得分 */
    private BigDecimal totalScore;

    /** 能力等级(A/B/C/D/E) */
    private String abilityLevel;

    /** 评估日期 */
    private LocalDate evaluateDate;

    private LocalDateTime createTime;
}
