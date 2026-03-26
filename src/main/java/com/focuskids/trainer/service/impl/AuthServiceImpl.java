package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.mapper.SysUserMapper;
import com.focuskids.trainer.service.AuthService;
import com.focuskids.trainer.util.JwtUtil;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public Map<String, Object> register(String phone, String password, Integer userType, String nickname) {
        // 检查手机号是否已注册
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_PHONE_EXISTS);
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setPhone(phone);
        user.setPassword(BCrypt.hashpw(password));
        user.setUserType(userType);
        user.setNickname(nickname);
        user.setStatus(1);
        user.setStarCount(0);
        userMapper.insert(user);

        // 生成token
        String token = jwtUtil.generateToken(user.getUserId(), user.getUserType());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        // 缓存token
        redisTemplate.opsForValue().set("token:" + user.getUserId(), token, 7, TimeUnit.DAYS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getUserId());
        result.put("userType", user.getUserType());
        return result;
    }

    @Override
    public Map<String, Object> login(String phone, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone);
        SysUser user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getUserType());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        redisTemplate.opsForValue().set("token:" + user.getUserId(), token, 7, TimeUnit.DAYS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getUserId());
        result.put("userType", user.getUserType());
        return result;
    }

    @Override
    @Transactional
    public void bindChild(Long parentId, Long childId) {
        // 检查家长已绑定儿童数量（最多3个）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getParentId, parentId);
        int bindCount = userMapper.selectCount(wrapper);
        if (bindCount >= 3) {
            throw new BusinessException(ErrorCode.CHILD_BIND_ERROR);
        }

        SysUser child = userMapper.selectById(childId);
        if (child == null || child.getUserType() != 1) {
            throw new BusinessException(ErrorCode.FAILED);
        }
        child.setParentId(parentId);
        userMapper.updateById(child);
    }

    @Override
    public Map<String, Object> refreshToken(String refreshToken) {
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String newToken = jwtUtil.generateToken(user.getUserId(), user.getUserType());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        redisTemplate.opsForValue().set("token:" + user.getUserId(), newToken, 7, TimeUnit.DAYS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("refreshToken", newRefreshToken);
        return result;
    }
}
