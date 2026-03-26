package com.focuskids.trainer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long userId;

    /** 用户类型(1:儿童 2:家长) */
    private Integer userType;

    /** 家长ID(儿童用户关联) */
    private Long parentId;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 密码(BCrypt加密) */
    private String password;

    /** 年龄 */
    private Integer age;

    /** 性别(0:未知 1:男 2:女) */
    private Integer gender;

    /** 年级(1-6) */
    private Integer grade;

    /** 星星总数 */
    private Integer starCount;

    /** 状态(0:禁用 1:正常) */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
