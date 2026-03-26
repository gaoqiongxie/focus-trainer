package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.TrainingConfig;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 训练控制器
 */
@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping("/config")
    public R<List<TrainingConfig>> getConfigList(@RequestParam(required = false) Integer trainingType) {
        return R.success(trainingService.getConfigList(trainingType));
    }

    @PostMapping("/start")
    public R<TrainingRecord> startTraining(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        Integer trainingType = (Integer) params.get("trainingType");
        Integer level = (Integer) params.get("level");
        Integer duration = params.get("duration") != null ? (Integer) params.get("duration") : null;
        return R.success(trainingService.startTraining(userId, trainingType, level, duration));
    }

    @PostMapping("/complete")
    public R<TrainingRecord> completeTraining(@RequestBody Map<String, Object> params) {
        Long recordId = Long.valueOf(params.get("recordId").toString());
        Integer actualDuration = (Integer) params.get("actualDuration");
        Integer interruptCount = params.get("interruptCount") != null ? (Integer) params.get("interruptCount") : 0;
        Double accuracy = params.get("accuracy") != null ? ((Number) params.get("accuracy")).doubleValue() : null;
        Integer score = params.get("score") != null ? (Integer) params.get("score") : 0;
        return R.success(trainingService.completeTraining(recordId, actualDuration, interruptCount, accuracy, score));
    }

    @PostMapping("/interrupt")
    public R<Void> interruptTraining(@RequestBody Map<String, Object> params) {
        Long recordId = Long.valueOf(params.get("recordId").toString());
        trainingService.interruptTraining(recordId);
        return R.success();
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> getStatistics(HttpServletRequest request,
                                                 @RequestParam(defaultValue = "week") String period) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(trainingService.getStatistics(userId, period));
    }

    @GetMapping("/records")
    public R<List<TrainingRecord>> getRecords(HttpServletRequest request,
                                               @RequestParam(required = false) Integer trainingType,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(trainingService.getRecords(userId, trainingType, page, size));
    }
}
