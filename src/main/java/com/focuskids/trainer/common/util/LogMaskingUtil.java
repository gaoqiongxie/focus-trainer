package com.focuskids.trainer.common.util;

import org.springframework.stereotype.Component;

/**
 * 日志脱敏工具类
 */
@Component
public class LogMaskingUtil {

    /**
     * 手机号脱敏: 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 姓名脱敏: 张*三
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.substring(0, 1) + "*" + name.substring(name.length() - 1);
    }

    /**
     * 身份证号脱敏: 110***********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }
}
