package com.focuskids.trainer.service;

import java.util.Map;

/**
 * 隐私合规服务
 */
public interface PrivacyService {

    /**
     * 导出用户所有数据（GDPR合规）
     * @param userId 用户ID
     * @return 导出记录信息（exportId, filePath）
     */
    Map<String, Object> exportUserData(Long userId);

    /**
     * 删除用户所有数据（GDPR合规，不可逆）
     * @param userId 用户ID
     * @param parentId 操作者（家长）ID，必须是孩子关联的家长
     */
    void deleteUserData(Long userId, Long parentId);
}
