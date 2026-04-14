package com.focuskids.trainer.common.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * 使用令牌桶算法，在指定时间窗口内限制请求次数
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 时间窗口内最大请求数，默认60 */
    int value() default 60;

    /** 时间窗口（秒），默认60秒 */
    int windowSeconds() default 60;
}
