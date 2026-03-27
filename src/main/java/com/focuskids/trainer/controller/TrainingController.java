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
        Object typeObj = params.get("trainingType");
        Object levelObj = params.get("level");
        if (typeObj == null || levelObj == null) {
            return R.error("trainingType和level不能为空");
        }
        Integer trainingType = ((Number) typeObj).intValue();
        Integer level = ((Number) levelObj).intValue();
        Integer duration = params.get("duration") != null ? ((Number) params.get("duration")).intValue() : null;
        return R.success(trainingService.startTraining(userId, trainingType, level, duration));
    }

    @PostMapping("/complete")
    public R<TrainingRecord> completeTraining(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        Object recordIdObj = params.get("recordId");
        Object durationObj = params.get("actualDuration");
        if (recordIdObj == null || durationObj == null) {
            return R.error("recordId和actualDuration不能为空");
        }
        Long recordId = Long.valueOf(recordIdObj.toString());
        Integer actualDuration = ((Number) durationObj).intValue();
        Integer interruptCount = params.get("interruptCount") != null ? ((Number) params.get("interruptCount")).intValue() : 0;
        Double accuracy = params.get("accuracy") != null ? ((Number) params.get("accuracy")).doubleValue() : null;
        Integer score = params.get("score") != null ? ((Number) params.get("score")).intValue() : 0;
        // 参数范围校验
        if (actualDuration <= 0 || actualDuration > 3600) {
            return R.error("实际训练时长不合法");
        }
        if (score < 0) {
            score = 0;
        }
        return R.success(trainingService.completeTraining(userId, recordId, actualDuration, interruptCount, accuracy, score));
    }

    @PostMapping("/interrupt")
    public R<Void> interruptTraining(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        Object recordIdObj = params.get("recordId");
        if (recordIdObj == null) {
            return R.error("recordId不能为空");
        }
        Long recordId = Long.valueOf(recordIdObj.toString());
        trainingService.interruptTraining(userId, recordId);
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
