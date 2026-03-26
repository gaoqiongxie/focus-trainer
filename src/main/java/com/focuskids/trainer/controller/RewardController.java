package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.Notification;
import com.focuskids.trainer.entity.UserStreak;
import com.focuskids.trainer.service.NotificationService;
import com.focuskids.trainer.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 激励与通知控制器
 */
@RestController
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;
    private final NotificationService notificationService;

    // ========== 激励相关 ==========

    @GetMapping("/reward/stars")
    public R<Integer> getStarCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(rewardService.getStarCount(userId));
    }

    @GetMapping("/reward/badges")
    public R<List<Map<String, Object>>> getBadges(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(rewardService.getBadges(userId));
    }

    @GetMapping("/reward/streak")
    public R<UserStreak> getStreak(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(rewardService.getStreak(userId));
    }

    @PostMapping("/reward/streak/update")
    public R<UserStreak> updateStreak(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(rewardService.updateStreak(userId));
    }

    // ========== 通知相关 ==========

    @GetMapping("/notification/list")
    public R<List<Notification>> getNotifications(HttpServletRequest request,
                                                    @RequestParam(required = false) Integer type,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(notificationService.getList(userId, type, page, size));
    }

    @PostMapping("/notification/read")
    public R<Void> markRead(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long notificationId = Long.valueOf(params.get("notificationId").toString());
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markRead(notificationId, userId);
        return R.success();
    }

    @PostMapping("/notification/read-all")
    public R<Void> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAllRead(userId);
        return R.success();
    }

    @GetMapping("/notification/unread-count")
    public R<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(notificationService.getUnreadCount(userId));
    }
}
