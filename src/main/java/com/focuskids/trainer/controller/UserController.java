package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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
    public R<Void> updateProfile(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }
        // 只允许更新安全字段：昵称、头像、年龄、性别、年级
        if (params.containsKey("nickname")) {
            user.setNickname((String) params.get("nickname"));
        }
        if (params.containsKey("avatar")) {
            user.setAvatar((String) params.get("avatar"));
        }
        if (params.containsKey("age")) {
            user.setAge(((Number) params.get("age")).intValue());
        }
        if (params.containsKey("gender")) {
            user.setGender(((Number) params.get("gender")).intValue());
        }
        if (params.containsKey("grade")) {
            user.setGrade(((Number) params.get("grade")).intValue());
        }
        userMapper.updateById(user);
        return R.success();
    }

    @GetMapping("/children")
    public R<List<SysUser>> getChildren(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(userMapper.selectChildrenByParentId(userId));
    }
}
