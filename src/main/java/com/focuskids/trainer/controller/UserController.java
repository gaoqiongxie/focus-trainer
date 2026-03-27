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
            String nickname = String.valueOf(params.get("nickname"));
            if (nickname.length() > 20) {
                return R.error("昵称不能超过20个字符");
            }
            user.setNickname(nickname);
        }
        if (params.containsKey("avatar")) {
            user.setAvatar(String.valueOf(params.get("avatar")));
        }
        if (params.containsKey("age")) {
            int age = ((Number) params.get("age")).intValue();
            if (age < 3 || age > 18) {
                return R.error("年龄需在3-18岁之间");
            }
            user.setAge(age);
        }
        if (params.containsKey("gender")) {
            user.setGender(((Number) params.get("gender")).intValue());
        }
        if (params.containsKey("grade")) {
            int grade = ((Number) params.get("grade")).intValue();
            if (grade < 1 || grade > 12) {
                return R.error("年级需在1-12之间");
            }
            user.setGrade(grade);
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
