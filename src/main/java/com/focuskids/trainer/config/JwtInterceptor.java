package com.focuskids.trainer.config;

import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT认证拦截器
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                Integer userType = jwtUtil.getUserTypeFromToken(token);
                request.setAttribute("userId", userId);
                request.setAttribute("userType", userType);
                return true;
            }
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
