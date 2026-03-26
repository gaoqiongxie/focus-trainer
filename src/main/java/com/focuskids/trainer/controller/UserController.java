package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;

    @GetMapping("/profile")
    public R<SysUser> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(userMapper.selectById(userId));
    }

    @PutMapping("/profile")
    public R<Void> updateProfile(HttpServletRequest request, @RequestBody SysUser user) {
        Long userId = (Long) request.getAttribute("userId");
        user.setUserId(userId);
        userMapper.updateById(user);
        return R.success();
    }

    @GetMapping("/children")
    public R<List<SysUser>> getChildren(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(userMapper.selectChildrenByParentId(userId));
    }
}
