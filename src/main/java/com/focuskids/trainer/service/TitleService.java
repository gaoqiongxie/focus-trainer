package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.UserTitle;
import com.focuskids.trainer.entity.UserTitleRecord;

import java.util.List;
import java.util.Map;

/**
 * 称号系统服务
 */
public interface TitleService {

    /**
     * 获取所有可用称号列表
     */
    List<UserTitle> listTitles();

    /**
     * 获取用户当前装备的称号
     */
    Map<String, Object> getCurrentTitle(Long userId);

    /**
     * 获取用户已解锁的称号列表
     */
    List<Map<String, Object>> getUnlockedTitles(Long userId);

    /**
     * 装备称号
     */
    void equipTitle(Long userId, Integer titleId);

    /**
     * 检查并解锁符合条件的称号（训练完成/评估后调用）
     * @return 新解锁的称号列表
     */
    List<UserTitle> checkAndUnlockTitles(Long userId);
}
