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
        String phone = (String) params.get("phone");
        String password = (String) params.get("password");
        Integer userType = (Integer) params.get("userType");
        String nickname = (String) params.get("nickname");
        return R.success(authService.register(phone, password, userType, nickname));
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, Object> params) {
        String phone = (String) params.get("phone");
        String password = (String) params.get("password");
        return R.success(authService.login(phone, password));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        // TODO: 从Redis清除token，强制失效
        return R.success();
    }

    @PostMapping("/refresh-token")
    public R<Map<String, Object>> refreshToken(@RequestBody Map<String, Object> params) {
        String refreshToken = (String) params.get("refreshToken");
        return R.success(authService.refreshToken(refreshToken));
    }

    @PostMapping("/child-bind")
    public R<Void> bindChild(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        // 从JWT中获取当前登录用户ID，防止IDOR漏洞
        Long parentId = (Long) request.getAttribute("userId");
        Long childId = Long.valueOf(params.get("childId").toString());
        authService.bindChild(parentId, childId);
        return R.success();
    }
}
