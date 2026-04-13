package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.TrainingSession;
import com.focuskids.trainer.service.TrainingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 训练会话控制器（防中断/断点续练）
 */
@RestController
@RequestMapping("/training/session")
@RequiredArgsConstructor
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    /**
     * 检查是否允许开始训练
     */
    @GetMapping("/check")
    public R<Map<String, Object>> checkAllowed(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(trainingSessionService.checkTrainingAllowed(userId));
    }

    /**
     * 创建训练会话
     */
    @PostMapping("/create")
    public R<TrainingSession> createSession(HttpServletRequest request,
                                             @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        Long recordId = Long.valueOf(params.get("recordId").toString());
        String sessionData = (String) params.get("sessionData");
        return R.success(trainingSessionService.createSession(userId, recordId, sessionData));
    }

    /**
     * 更新会话数据
     */
    @PutMapping("/update/{sessionId}")
    public R<Void> updateSessionData(HttpServletRequest request,
                                      @PathVariable Long sessionId,
                                      @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        String sessionData = (String) params.get("sessionData");
        trainingSessionService.updateSessionData(sessionId, userId, sessionData);
        return R.success();
    }

    /**
     * 暂停训练
     */
    @PutMapping("/pause/{sessionId}")
    public R<TrainingSession> pauseSession(HttpServletRequest request,
                                            @PathVariable Long sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(trainingSessionService.pauseSession(sessionId, userId));
    }

    /**
     * 恢复训练
     */
    @PutMapping("/resume/{sessionId}")
    public R<TrainingSession> resumeSession(HttpServletRequest request,
                                             @PathVariable Long sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(trainingSessionService.resumeSession(sessionId, userId));
    }

    /**
     * 结束训练会话
     */
    @PutMapping("/end/{sessionId}")
    public R<Void> endSession(HttpServletRequest request,
                               @PathVariable Long sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        trainingSessionService.endSession(sessionId, userId);
        return R.success();
    }

    /**
     * 获取活跃会话
     */
    @GetMapping("/active")
    public R<TrainingSession> getActiveSession(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        TrainingSession session = trainingSessionService.getActiveSession(userId);
        return R.success(session);
    }
}
