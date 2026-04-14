package com.focuskids.trainer.common.interceptor;

import com.focuskids.trainer.common.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器 - 基于Redis实现滑动窗口限流
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final boolean rateLimitEnabled;
    private final int defaultRequestsPerMinute;

    public RateLimitInterceptor(
            StringRedisTemplate redisTemplate,
            @Value("${rate-limit.enabled:true}") boolean rateLimitEnabled,
            @Value("${rate-limit.requests-per-minute:60}") int defaultRequestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.rateLimitEnabled = rateLimitEnabled;
        this.defaultRequestsPerMinute = defaultRequestsPerMinute;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!rateLimitEnabled) {
            return true;
        }

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        RateLimit rateLimit = method.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        int maxRequests = rateLimit.value() > 0 ? rateLimit.value() : defaultRequestsPerMinute;
        int windowSeconds = rateLimit.windowSeconds();

        String clientIp = getClientIp(request);
        String redisKey = "rate_limit:" + method.getMethod().getName() + ":" + clientIp;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);
            if (currentCount == null) {
                return true;
            }

            if (currentCount == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }

            if (currentCount > maxRequests) {
                log.warn("Rate limit exceeded for IP {} on {}", clientIp, request.getRequestURI());
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":9003,\"msg\":\"请求过于频繁，请稍后再试\"}");
                return false;
            }
        } catch (Exception e) {
            // Redis异常时降级放行，不影响业务
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
