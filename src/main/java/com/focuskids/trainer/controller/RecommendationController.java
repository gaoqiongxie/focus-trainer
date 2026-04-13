package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.service.DifficultyRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 训练推荐控制器
 */
@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
public class RecommendationController {

    private final DifficultyRecommendationService recommendationService;

    /**
     * 获取个性化推荐训练列表
     */
    @GetMapping("/recommend")
    public R<List<Map<String, Object>>> getRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(recommendationService.getRecommendations(userId));
    }

    /**
     * 获取指定类型的推荐难度
     */
    @GetMapping("/recommend/{trainingType}")
    public R<Map<String, Object>> recommendForType(HttpServletRequest request,
                                                     @PathVariable Integer trainingType) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(recommendationService.recommendForType(userId, trainingType));
    }
}
