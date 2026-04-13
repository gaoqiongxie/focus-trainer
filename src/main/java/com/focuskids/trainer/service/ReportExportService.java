package com.focuskids.trainer.service;

import java.util.Map;

/**
 * 报告导出服务
 */
public interface ReportExportService {

    /**
     * 导出训练报告PDF
     * @param userId 用户ID（孩子）
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 导出结果（filePath, fileSize）
     */
    Map<String, Object> exportPdf(Long userId, String startDate, String endDate);
}
