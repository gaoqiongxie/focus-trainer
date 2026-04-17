package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.UserAbility;
import com.focuskids.trainer.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估控制器
 */
@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * 初始化评估
     */
    @PostMapping("/initialize")
    public R<Void> initEvaluation(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        evaluationService.initEvaluation(userId);
        return R.success();
    }

    /**
     * 生成/更新评估结果
     */
    @PostMapping("/submit")
    public R<UserAbility> submitEvaluation(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(evaluationService.generateEvaluation(userId));
    }

    /**
     * 获取最新评估结果
     */
    @GetMapping("/result")
    public R<UserAbility> getResult(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(evaluationService.getLatestEvaluation(userId));
    }

    /**
     * 获取评估历史
     */
    @GetMapping("/history")
    public R<List<UserAbility>> getHistory(HttpServletRequest request,
                                            @RequestParam(defaultValue = "10") int limit) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(evaluationService.getEvaluationHistory(userId, limit));
    }

    /**
     * 获取能力引导推荐（基于最新评估，推荐薄弱项训练）
     * 返回格式: { enabled: bool, ability: UserAbility|null, recommendations: [...] }
     */
    @GetMapping("/guide")
    public R<Map<String, Object>> getGuide(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserAbility ability = evaluationService.getLatestEvaluation(userId);
        List<Map<String, Object>> recommendations = evaluationService.getRecommendations(ability);
        boolean needsEvaluation = ability == null;
        Map<String, Object> result = new HashMap<>();
        result.put("needsEvaluation", needsEvaluation);
        result.put("ability", ability != null ? ability : new HashMap<>());
        result.put("recommendations", recommendations);
        result.put("hasAbility", ability != null);
        return R.success(result);
    }
}
