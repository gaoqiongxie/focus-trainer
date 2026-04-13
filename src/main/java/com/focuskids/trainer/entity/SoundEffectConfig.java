package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 音效配置表
 */
@Data
@TableName("sound_effect_config")
public class SoundEffectConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer soundId;

    /** 音效唯一标识 */
    private String soundKey;

    /** 音效名称 */
    private String name;

    /** 类别: reward/game/ui/animal */
    private String category;

    /** 音效文件名 */
    private String fileName;

    /** 时长(毫秒) */
    private Integer durationMs;

    /** 描述 */
    private String description;

    /** 是否启用 */
    private Integer isActive;

    /** 展示顺序 */
    private Integer displayOrder;

    private LocalDateTime createTime;
}
