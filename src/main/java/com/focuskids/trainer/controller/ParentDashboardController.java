package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.service.ParentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 家长端增强控制器
 */
@RestController
@RequestMapping("/parent/dashboard")
@RequiredArgsConstructor
public class ParentDashboardController {

    private final ParentDashboardService parentDashboardService;

    /**
     * 获取所有孩子的汇总数据
     */
    @GetMapping("/children")
    public R<List<Map<String, Object>>> getChildrenSummary(HttpServletRequest request) {
        Long parentId = (Long) request.getAttribute("userId");
        return R.success(parentDashboardService.getChildrenSummary(parentId));
    }

    /**
     * 获取训练趋势分析
     */
    @GetMapping("/trend/{childId}")
    public R<Map<String, Object>> getTrendAnalysis(HttpServletRequest request,
                                                    @PathVariable Long childId,
                                                    @RequestParam(defaultValue = "4") int weeks) {
        return R.success(parentDashboardService.getTrendAnalysis(childId, weeks));
    }

    /**
     * 获取专家建议
     */
    @GetMapping("/advice/{childId}")
    public R<List<Map<String, Object>>> getExpertAdvice(HttpServletRequest request,
                                                          @PathVariable Long childId) {
        return R.success(parentDashboardService.getExpertAdvice(childId));
    }
}
