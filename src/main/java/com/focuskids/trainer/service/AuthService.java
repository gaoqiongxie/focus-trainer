package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.SysUser;

import java.util.Map;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 用户注册
     */
    Map<String, Object> register(String phone, String password, Integer userType, String nickname);

    /**
     * 用户登录
     */
    Map<String, Object> login(String phone, String password);

    /**
     * 家长绑定儿童
     */
    void bindChild(Long parentId, Long childId);

    /**
     * 用户登出
     */
    void logout(Long userId);

    /**
     * 刷新token
     */
    Map<String, Object> refreshToken(String refreshToken);
}
