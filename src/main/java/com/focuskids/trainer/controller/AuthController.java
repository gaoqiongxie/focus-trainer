package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody Map<String, Object> params) {
        String phone = String.valueOf(params.get("phone"));
        String password = String.valueOf(params.get("password"));
        Integer userType = params.get("userType") != null ? ((Number) params.get("userType")).intValue() : 1;
        String nickname = params.get("nickname") != null ? String.valueOf(params.get("nickname")) : "";
        // 输入校验
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return R.error("请输入正确的手机号");
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            return R.error("密码长度需在6-20位之间");
        }
        if (nickname.length() > 20) {
            return R.error("昵称不能超过20个字符");
        }
        return R.success(authService.register(phone, password, userType, nickname));
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, Object> params) {
        String phone = String.valueOf(params.get("phone"));
        String password = String.valueOf(params.get("password"));
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return R.error("请输入正确的手机号");
        }
        if (password == null || password.isEmpty()) {
            return R.error("请输入密码");
        }
        return R.success(authService.login(phone, password));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        authService.logout(userId);
        return R.success();
    }

    @PostMapping("/refresh-token")
    public R<Map<String, Object>> refreshToken(@RequestBody Map<String, Object> params) {
        String refreshToken = (String) params.get("refreshToken");
        return R.success(authService.refreshToken(refreshToken));
    }

    @PostMapping("/child-bind")
    public R<Void> bindChild(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long parentId = (Long) request.getAttribute("userId");
        Object childIdObj = params.get("childId");
        if (childIdObj == null) {
            return R.error("childId不能为空");
        }
        Long childId = Long.valueOf(childIdObj.toString());
        authService.bindChild(parentId, childId);
        return R.success();
    }
}
