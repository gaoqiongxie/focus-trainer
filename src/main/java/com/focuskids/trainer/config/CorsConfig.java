package com.focuskids.trainer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS跨域配置
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:*,http://10.0.2.2:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 限制允许的源，默认仅本地和模拟器，生产环境通过配置指定具体域名
        List<String> origins = Arrays.asList(StringUtils.trimArrayElements(
                allowedOrigins.split(",")));
        for (String origin : origins) {
            config.addAllowedOriginPattern(origin.trim());
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
