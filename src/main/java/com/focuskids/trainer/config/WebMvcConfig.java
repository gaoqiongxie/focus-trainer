package com.focuskids.trainer.config;

import com.focuskids.trainer.common.interceptor.AccessLogInterceptor;
import com.focuskids.trainer.common.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置 - 注册JWT拦截器、访问日志拦截器和限流拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AccessLogInterceptor accessLogInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 访问日志拦截器 - 记录所有请求（最先执行）
        registry.addInterceptor(accessLogInterceptor)
                .addPathPatterns("/**");

        // 限流拦截器 - 针对认证等敏感接口
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/auth/**");

        // JWT认证拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/register",
                        "/auth/login",
                        "/auth/refresh-token",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs"
                );
    }
}
