package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.Notification;

import java.util.List;

/**
 * 通知服务
 */
public interface NotificationService {

    /**
     * 获取通知列表
     */
    List<Notification> getList(Long userId, Integer type, int page, int size);

    /**
     * 标记已读
     */
    void markRead(Long notificationId, Long userId);

    /**
     * 全部标记已读
     */
    void markAllRead(Long userId);

    /**
     * 发送通知
     */
    void send(Long userId, Integer type, String title, String content);

    /**
     * 获取未读数量
     */
    int getUnreadCount(Long userId);
}
