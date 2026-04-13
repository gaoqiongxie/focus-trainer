package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.UserTitle;
import com.focuskids.trainer.service.TitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 称号控制器
 */
@RestController
@RequestMapping("/title")
@RequiredArgsConstructor
public class TitleController {

    private final TitleService titleService;

    /**
     * 获取所有可用称号
     */
    @GetMapping("/list")
    public R<List<UserTitle>> listTitles() {
        return R.success(titleService.listTitles());
    }

    /**
     * 获取当前装备的称号
     */
    @GetMapping("/current")
    public R<Map<String, Object>> getCurrentTitle(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(titleService.getCurrentTitle(userId));
    }

    /**
     * 获取已解锁的称号列表
     */
    @GetMapping("/unlocked")
    public R<List<Map<String, Object>>> getUnlockedTitles(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(titleService.getUnlockedTitles(userId));
    }

    /**
     * 装备称号
     */
    @PostMapping("/equip/{titleId}")
    public R<Void> equipTitle(HttpServletRequest request, @PathVariable Integer titleId) {
        Long userId = (Long) request.getAttribute("userId");
        titleService.equipTitle(userId, titleId);
        return R.success();
    }

    /**
     * 检查并解锁称号
     */
    @PostMapping("/check")
    public R<List<UserTitle>> checkAndUnlock(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(titleService.checkAndUnlockTitles(userId));
    }
}
