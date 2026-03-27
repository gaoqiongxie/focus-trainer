package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.TrainingConfig;
import com.focuskids.trainer.entity.TrainingRecord;

import java.util.List;
import java.util.Map;

/**
 * 训练服务
 */
public interface TrainingService {

    /**
     * 获取训练配置列表
     */
    List<TrainingConfig> getConfigList(Integer trainingType);

    /**
     * 开始训练
     */
    TrainingRecord startTraining(Long userId, Integer trainingType, Integer level, Integer duration);

    /**
     * 完成训练
     */
    TrainingRecord completeTraining(Long userId, Long recordId, Integer actualDuration, Integer interruptCount,
                                     Double accuracy, Integer score);

    /**
     * 中断训练
     */
    void interruptTraining(Long userId, Long recordId);

    /**
     * 获取训练统计
     */
    Map<String, Object> getStatistics(Long userId, String period);

    /**
     * 获取训练记录列表
     */
    List<TrainingRecord> getRecords(Long userId, Integer trainingType, int page, int size);
}
