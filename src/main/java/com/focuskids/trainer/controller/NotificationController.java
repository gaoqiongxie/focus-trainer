package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.Notification;
import com.focuskids.trainer.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping("/list")
    public R<List<Notification>> getList(HttpServletRequest request,
                                          @RequestParam(required = false) Integer type,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(notificationService.getList(userId, type, page, size));
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    public R<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(notificationService.getUnreadCount(userId));
    }

    /**
     * 标记已读
     */
    @PutMapping("/markRead/{notificationId}")
    public R<Void> markRead(HttpServletRequest request, @PathVariable Long notificationId) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markRead(notificationId, userId);
        return R.success();
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/markAllRead")
    public R<Void> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAllRead(userId);
        return R.success();
    }

    /**
     * 发送通知（系统调用）
     */
    @PostMapping("/send")
    public R<Void> send(@RequestBody Map<String, Object> params) {
        Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
        Integer type = params.get("type") != null ? ((Number) params.get("type")).intValue() : 1;
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        if (userId == null || title == null || content == null) {
            return R.error("userId、title、content不能为空");
        }
        notificationService.send(userId, type, title, content);
        return R.success();
    }
}
