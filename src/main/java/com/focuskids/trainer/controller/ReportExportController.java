package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.service.ParentReportService;
import com.focuskids.trainer.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 报告导出控制器
 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportService reportExportService;
    private final ParentReportService parentReportService;

    /**
     * 导出训练报告PDF
     * 从 JWT token 获取家长ID，childId 指定要导出的孩子
     */
    @GetMapping("/export")
    public R<Map<String, Object>> exportPdf(HttpServletRequest request,
                                             @RequestParam(required = false) Long childId,
                                             @RequestParam String startDate,
                                             @RequestParam String endDate) {
        Long parentUserId = (Long) request.getAttribute("userId");
        Long targetChildId = parentReportService.resolveChildId(parentUserId, childId);
        return R.success(reportExportService.exportPdf(targetChildId, startDate, endDate));
    }

    /**
     * 下载PDF报告
     */
    @GetMapping("/download")
    public void downloadPdf(@RequestParam String filePath,
                             HttpServletResponse response) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                response.setStatus(404);
                return;
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + file.getName() + "\"");
            response.setContentLengthLong(file.length());

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (Exception e) {
            response.setStatus(500);
        }
    }
}
