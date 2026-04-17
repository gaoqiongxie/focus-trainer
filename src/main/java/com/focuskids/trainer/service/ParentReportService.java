package com.focuskids.trainer.service;

import java.util.List;
import java.util.Map;

/**
 * 家长端数据报告服务
 */
public interface ParentReportService {

    /**
     * 获取训练总览仪表板
     */
    Map<String, Object> getDashboard(Long parentUserId, Long childId);

    /**
     * 获取训练趋势数据
     */
    List<Map<String, Object>> getTrainingTrend(Long parentUserId, Long childId, int days);

    /**
     * 获取能力分析（各维度得分+雷达图数据）
     */
    Map<String, Object> getAbilityAnalysis(Long parentUserId, Long childId);

    /**
     * 获取训练记录明细
     */
    Map<String, Object> getDetailedRecords(Long parentUserId, Long childId, Integer trainingType, int page, int size);

    /**
     * 获取周报
     */
    Map<String, Object> getWeeklyReport(Long parentUserId, Long childId);

    /**
     * 解析儿童ID（childId为空时返回默认孩子）
     */
    Long resolveChildId(Long parentUserId, Long childId);
}
