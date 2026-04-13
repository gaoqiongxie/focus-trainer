package com.focuskids.trainer.service;

import java.util.List;
import java.util.Map;

/**
 * 家长端增强服务
 */
public interface ParentDashboardService {

    /**
     * 获取家长下所有孩子的汇总数据
     * @param parentId 家长ID
     * @return 孩子列表及各自统计数据
     */
    List<Map<String, Object>> getChildrenSummary(Long parentId);

    /**
     * 获取训练效果趋势分析
     * @param userId 孩子ID
     * @param weeks 周数（最近N周）
     * @return 趋势数据
     */
    Map<String, Object> getTrendAnalysis(Long userId, int weeks);

    /**
     * 获取专家建议
     * @param userId 孩子ID
     * @return 专家建议列表
     */
    List<Map<String, Object>> getExpertAdvice(Long userId);
}
