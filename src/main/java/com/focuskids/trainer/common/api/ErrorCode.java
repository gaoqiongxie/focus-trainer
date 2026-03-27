package com.focuskids.trainer.common.api;

/**
 * 业务错误码
 */
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),

    // 用户相关 1001-1099
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_ALREADY_EXISTS(1003, "用户已存在"),
    USER_DISABLED(1004, "用户已被禁用"),
    USER_PHONE_EXISTS(1005, "手机号已注册"),
    CHILD_BIND_ERROR(1006, "绑定儿童失败，最多绑定3个"),

    // 训练相关 2001-2099
    TRAINING_NOT_FOUND(2001, "训练记录不存在"),
    TRAINING_IN_PROGRESS(2002, "已有进行中的训练"),
    TRAINING_CONFIG_ERROR(2003, "训练配置不存在"),
    TRAINING_DURATION_ERROR(2004, "训练时长不合法"),
    TRAINING_ALREADY_COMPLETED(2005, "训练已完成或已中断，无法重复操作"),

    // 评估相关 3001-3099
    EVALUATION_NOT_FOUND(3001, "评估记录不存在"),
    EVALUATION_IN_PROGRESS(3002, "已有进行中的评估"),

    // 系统相关 9001-9999
    SYSTEM_ERROR(9001, "系统繁忙，请稍后重试"),
    FILE_UPLOAD_ERROR(9002, "文件上传失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
