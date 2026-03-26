package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.service.ParentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 家长端数据报告控制器
 * 家长可查看所绑定儿童的训练数据报告
 */
@RestController
@RequestMapping("/parent/report")
@RequiredArgsConstructor
public class ParentReportController {

    private final ParentReportService parentReportService;

    /**
     * 获取训练总览仪表板
     * 包含今日/本周/本月统计数据
     */
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboard(HttpServletRequest request,
                                                 @RequestParam(required = false) Long childId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(parentReportService.getDashboard(userId, childId));
    }

    /**
     * 获取训练趋势数据（按天统计）
     */
    @GetMapping("/trend")
    public R<List<Map<String, Object>>> getTrend(HttpServletRequest request,
                                                   @RequestParam(required = false) Long childId,
                                                   @RequestParam(defaultValue = "7") int days) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(parentReportService.getTrainingTrend(userId, childId, days));
    }

    /**
     * 获取各训练类型的能力分析
     */
    @GetMapping("/ability")
    public R<Map<String, Object>> getAbilityAnalysis(HttpServletRequest request,
                                                       @RequestParam(required = false) Long childId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(parentReportService.getAbilityAnalysis(userId, childId));
    }

    /**
     * 获取训练记录明细
     */
    @GetMapping("/records")
    public R<Map<String, Object>> getDetailedRecords(HttpServletRequest request,
                                                       @RequestParam(required = false) Long childId,
                                                       @RequestParam(required = false) Integer trainingType,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(parentReportService.getDetailedRecords(userId, childId, trainingType, page, size));
    }

    /**
     * 获取训练周报
     */
    @GetMapping("/weekly")
    public R<Map<String, Object>> getWeeklyReport(HttpServletRequest request,
                                                    @RequestParam(required = false) Long childId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(parentReportService.getWeeklyReport(userId, childId));
    }
}
