package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据导出记录表（隐私合规）
 */
@Data
@TableName("data_export_record")
public class DataExportRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long exportId;

    /** 用户ID */
    private Long userId;

    /** 导出文件路径 */
    private String filePath;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 状态(0:生成中 1:完成 2:失败) */
    private Integer status;

    /** 过期时间 */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
