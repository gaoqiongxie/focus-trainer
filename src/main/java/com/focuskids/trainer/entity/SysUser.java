package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /** 用户类型(1:儿童 2:家长) */
    private Integer userType;

    /** 家长ID */
    private Long parentId;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 密码 */
    @TableField(select = false)
    private String password;

    /** 年龄 */
    private Integer age;

    /** 性别(0:未知 1:男 2:女) */
    private Integer gender;

    /** 年级 */
    private Integer grade;

    /** 星星总数 */
    private Integer starCount;

    /** 状态(0:禁用 1:正常) */
    private Integer status;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
