package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.UserAbility;
import com.focuskids.trainer.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 评估控制器
 */
@RestController
@RequestMapping("/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/initialize")
    public R<Void> initEvaluation(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        evaluationService.initEvaluation(userId);
        return R.success();
    }

    @PostMapping("/submit")
    public R<UserAbility> submitEvaluation(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(evaluationService.generateEvaluation(userId));
    }

    @GetMapping("/result")
    public R<UserAbility> getResult(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(evaluationService.getLatestEvaluation(userId));
    }

    @GetMapping("/history")
    public R<List<UserAbility>> getHistory(HttpServletRequest request,
                                            @RequestParam(defaultValue = "10") int limit) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(evaluationService.getEvaluationHistory(userId, limit));
    }
}
