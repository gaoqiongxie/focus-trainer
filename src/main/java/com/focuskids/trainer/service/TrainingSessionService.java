package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.TrainingSession;

import java.util.Map;

/**
 * 训练防中断服务
 */
public interface TrainingSessionService {

    /**
     * 创建训练会话
     * @param userId 用户ID
     * @param recordId 训练记录ID
     * @param sessionData 初始会话数据(JSON)
     * @return 训练会话
     */
    TrainingSession createSession(Long userId, Long recordId, String sessionData);

    /**
     * 更新会话状态数据
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param sessionData 会话数据(JSON)
     */
    void updateSessionData(Long sessionId, Long userId, String sessionData);

    /**
     * 暂停训练
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    TrainingSession pauseSession(Long sessionId, Long userId);

    /**
     * 恢复训练
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    TrainingSession resumeSession(Long sessionId, Long userId);

    /**
     * 结束训练会话
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    void endSession(Long sessionId, Long userId);

    /**
     * 获取活跃会话（进行中或已暂停）
     * @param userId 用户ID
     * @return 活跃会话，无则返回null
     */
    TrainingSession getActiveSession(Long userId);

    /**
     * 检查是否允许训练（家长锁定/每日时长限制）
     * @param userId 用户ID
     * @return 检查结果（allowed, reason）
     */
    Map<String, Object> checkTrainingAllowed(Long userId);
}
