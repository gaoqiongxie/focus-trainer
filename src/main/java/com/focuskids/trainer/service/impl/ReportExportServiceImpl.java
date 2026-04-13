package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.entity.*;
import com.focuskids.trainer.mapper.*;
import com.focuskids.trainer.service.ReportExportService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 报告导出服务实现（PDF生成）
 * 使用 OpenPDF (librepdf/openpdf) — Java 8 兼容
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportServiceImpl implements ReportExportService {

    private final SysUserMapper userMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final UserAbilityMapper userAbilityMapper;
    private final DataExportRecordMapper dataExportRecordMapper;

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    private static final Map<Integer, String> TYPE_NAMES = new HashMap<>();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    static {
        TYPE_NAMES.put(1, "专注时长");
        TYPE_NAMES.put(2, "视觉追踪");
        TYPE_NAMES.put(3, "听觉专注");
        TYPE_NAMES.put(4, "记忆训练");
        TYPE_NAMES.put(21, "数字闪现");
        TYPE_NAMES.put(41, "卡片配对");
    }

    @Override
    @Transactional
    public Map<String, Object> exportPdf(Long userId, String startDate, String endDate) {
        // 1. 校验用户
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 2. 解析日期
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "开始日期不能晚于结束日期");
        }
        if (start.plusYears(1).isBefore(end)) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "日期范围不能超过1年");
        }

        // 3. 查询训练记录
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);
        LambdaQueryWrapper<TrainingRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(TrainingRecord::getUserId, userId)
                     .ge(TrainingRecord::getStartTime, startDateTime)
                     .le(TrainingRecord::getStartTime, endDateTime)
                     .orderByAsc(TrainingRecord::getStartTime);
        List<TrainingRecord> records = trainingRecordMapper.selectList(recordWrapper);

        // 4. 查询最新能力评估
        LambdaQueryWrapper<UserAbility> abilityWrapper = new LambdaQueryWrapper<>();
        abilityWrapper.eq(UserAbility::getUserId, userId)
                      .orderByDesc(UserAbility::getEvaluateDate)
                      .last("LIMIT 1");
        UserAbility ability = userAbilityMapper.selectOne(abilityWrapper);

        // 5. 生成 PDF
        String fileName = String.format("report_%s_%s_%s.pdf",
                userId, start.format(FILE_DATE_FMT), end.format(FILE_DATE_FMT));
        Path reportDir = Paths.get(uploadPath, "reports");
        try {
            Files.createDirectories(reportDir);
        } catch (Exception e) {
            log.error("创建报告目录失败: {}", reportDir, e);
            // fallback 到临时目录
            reportDir = Files.createTempDirectory("focus-reports");
        }
        File pdfFile = reportDir.resolve(fileName).toFile();

        try {
            generatePdf(user, records, ability, start, end, pdfFile);
        } catch (Exception e) {
            log.error("生成PDF失败", e);
            throw new BusinessException(ErrorCode.FAILED, "生成报告失败: " + e.getMessage());
        }

        // 6. 记录导出
        DataExportRecord exportRecord = new DataExportRecord();
        exportRecord.setUserId(userId);
        exportRecord.setFilePath(pdfFile.getAbsolutePath());
        exportRecord.setFileSize(pdfFile.length());
        exportRecord.setStatus(1);
        exportRecord.setExpireTime(LocalDateTime.now().plusDays(7));
        exportRecord.setCreateTime(LocalDateTime.now());
        dataExportRecordMapper.insert(exportRecord);

        // 7. 返回
        Map<String, Object> result = new HashMap<>();
        result.put("exportId", exportRecord.getExportId());
        result.put("filePath", pdfFile.getAbsolutePath());
        result.put("fileName", fileName);
        result.put("fileSize", pdfFile.length());
        result.put("expireTime", exportRecord.getExpireTime());
        log.info("[报告导出] 用户{}导出报告成功: {}", userId, fileName);

        return result;
    }

    /**
     * 生成PDF文档
     */
    private void generatePdf(SysUser user, List<TrainingRecord> records,
                              UserAbility ability, LocalDate start, LocalDate end,
                              File pdfFile) throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        document.open();

        // 颜色定义
        Color primaryColor = new Color(76, 175, 80);    // 绿色
        Color headerBg = new Color(232, 245, 233);       // 浅绿背景
        Color subHeaderBg = new Color(200, 230, 201);    // 中绿背景

        // ===== 封面标题 =====
        Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, primaryColor);
        Paragraph title = new Paragraph("专注力训练报告", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // 副标题
        Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY);
        String subtitleText = String.format("%s  ·  %s 至 %s",
                user.getNickname() != null ? user.getNickname() : "用户",
                start.format(DATE_FMT), end.format(DATE_FMT));
        Paragraph subtitle = new Paragraph(subtitleText, subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        // ===== 一、训练总览 =====
        addSectionTitle(document, "一、训练总览", primaryColor, headerBg);
        document.add(new Paragraph("\n"));

        Map<String, Object> summary = calcSummary(records);
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidthPercentage(100);
        float[] summaryWidths = {1f, 1f, 1f, 1f};
        summaryTable.setWidths(summaryWidths);

        addSummaryCell(summaryTable, "训练次数", String.valueOf(summary.get("totalSessions")), primaryColor);
        addSummaryCell(summaryTable, "累计时长", (String) summary.get("totalDuration"), primaryColor);
        addSummaryCell(summaryTable, "平均正确率", (String) summary.get("avgAccuracy"), primaryColor);
        addSummaryCell(summaryTable, "获得星星", String.valueOf(summary.get("totalStars")), primaryColor);
        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // ===== 二、能力评估 =====
        addSectionTitle(document, "二、能力评估", primaryColor, headerBg);
        document.add(new Paragraph("\n"));

        if (ability != null) {
            PdfPTable abilityTable = new PdfPTable(2);
            abilityTable.setWidthPercentage(80);
            float[] abilityWidths = {2f, 1f};
            abilityTable.setWidths(abilityWidths);

            addAbilityRow(abilityTable, "专注时长", ability.getAttentionDuration());
            addAbilityRow(abilityTable, "视觉注意力", ability.getVisualAttention());
            addAbilityRow(abilityTable, "听觉注意力", ability.getAuditoryAttention());
            addAbilityRow(abilityTable, "工作记忆", ability.getWorkingMemory());
            addAbilityRow(abilityTable, "抑制控制", ability.getInhibitoryControl());

            PdfPCell levelCell = new PdfPCell(new Phrase(
                    "综合等级: " + (ability.getAbilityLevel() != null ? ability.getAbilityLevel() : "—"),
                    new Font(Font.HELVETICA, 12, Font.BOLD, primaryColor)));
            levelCell.setColspan(2);
            levelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            levelCell.setPadding(12);
            levelCell.setBackgroundColor(new Color(255, 243, 224));
            abilityTable.addCell(levelCell);

            document.add(abilityTable);
        } else {
            document.add(new Paragraph("暂无能力评估数据，请先完成一次完整的能力评估训练。",
                    new Font(Font.HELVETICA, 11, Font.ITALIC, Color.GRAY)));
        }
        document.add(new Paragraph("\n"));

        // ===== 三、训练明细 =====
        addSectionTitle(document, "三、训练明细", primaryColor, headerBg);
        document.add(new Paragraph("\n"));

        if (records == null || records.isEmpty()) {
            document.add(new Paragraph("该时间段内暂无训练记录。",
                    new Font(Font.HELVETICA, 11, Font.ITALIC, Color.GRAY)));
        } else {
            PdfPTable detailTable = new PdfPTable(5);
            detailTable.setWidthPercentage(100);
            float[] detailWidths = {2.5f, 1.5f, 1.5f, 1.5f, 1.5f};
            detailTable.setWidths(detailWidths);

            // 表头
            String[] headers = {"日期", "训练类型", "难度", "正确率", "获得星星"};
            for (String h : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
                headerCell.setBackgroundColor(primaryColor);
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(8);
                detailTable.addCell(headerCell);
            }

            // 数据行
            int rowCount = 0;
            for (TrainingRecord r : records) {
                boolean isEven = (rowCount % 2 == 0);
                Color rowBg = isEven ? Color.WHITE : new Color(245, 245, 245);

                addDetailCell(detailTable, formatDate(r.getStartTime()), rowBg);
                addDetailCell(detailTable, TYPE_NAMES.getOrDefault(r.getTrainingType(), String.valueOf(r.getTrainingType())), rowBg);
                addDetailCell(detailTable, getLevelName(r.getLevel()), rowBg);
                addDetailCell(detailTable, r.getAccuracy() != null ? r.getAccuracy().setScale(1, RoundingMode.HALF_UP) + "%" : "—", rowBg);
                addDetailCell(detailTable, r.getStarReward() != null ? String.valueOf(r.getStarReward()) : "0", rowBg);
                rowCount++;
            }
            document.add(detailTable);
        }

        // ===== 四、建议 =====
        document.add(new Paragraph("\n"));
        addSectionTitle(document, "四、训练建议", primaryColor, headerBg);
        document.add(new Paragraph("\n"));

        String advice = generateAdvice(ability, records);
        Font adviceFont = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(33, 33, 33));
        Paragraph advicePara = new Paragraph(advice, adviceFont);
        advicePara.setFirstLineIndent(20);
        advicePara.setSpacingAfter(10);
        document.add(advicePara);

        // ===== 页脚 =====
        document.add(new Paragraph("\n\n"));
        Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);
        Paragraph footer = new Paragraph(
                "本报告由 Focus Trainer 自动生成  ·  生成时间: " + LocalDateTime.now().format(DATE_FMT),
                footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }

    /**
     * 添加分节标题
     */
    private void addSectionTitle(Document document, String text, Color textColor, Color bgColor) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, textColor);
        Paragraph section = new Paragraph(text, sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(6);
        document.add(section);

        // 分隔线
        PdfContentByte canvas = PdfWriter.getInstance(document, new FileOutputStream(File.createTempFile("tmp", ".pdf"))).getDirectContent();
        // 分隔线使用普通方式
        LineSeparator line = new LineSeparator();
        line.setLineColor(new Color(200, 200, 200));
        document.add(new Phrase("\n"));
    }

    /**
     * 添加汇总格
     */
    private void addSummaryCell(PdfPTable table, String label, String value, Color accentColor) {
        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font valueFont = new Font(Font.HELVETICA, 16, Font.BOLD, accentColor);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p = new Paragraph();
        p.add(new Phrase(label + "\n", labelFont));
        p.add(new Phrase(value, valueFont));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);

        table.addCell(cell);
    }

    /**
     * 添加能力行
     */
    private void addAbilityRow(PdfPTable table, String name, BigDecimal value) {
        Font nameFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
        Font valueFont = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(76, 175, 80));

        PdfPCell nameCell = new PdfPCell(new Phrase(name, nameFont));
        nameCell.setPadding(10);
        nameCell.setBorder(Rectangle.BOTTOM);
        nameCell.setBorderColor(new Color(220, 220, 220));
        table.addCell(nameCell);

        String displayValue = value != null ? value.setScale(1, RoundingMode.HALF_UP) + "分" : "—";
        PdfPCell valueCell = new PdfPCell(new Phrase(displayValue, valueFont));
        valueCell.setPadding(10);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(new Color(220, 220, 220));
        table.addCell(valueCell);
    }

    /**
     * 添加明细格
     */
    private void addDetailCell(PdfPTable table, String text, Color bgColor) {
        Font font = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(7);
        table.addCell(cell);
    }

    /**
     * 计算汇总数据
     */
    private Map<String, Object> calcSummary(List<TrainingRecord> records) {
        Map<String, Object> result = new HashMap<>();
        int totalSessions = records.size();
        int totalSeconds = 0;
        BigDecimal totalAccuracy = BigDecimal.ZERO;
        int accuracyCount = 0;
        int totalStars = 0;

        for (TrainingRecord r : records) {
            if (r.getActualDuration() != null) {
                totalSeconds += r.getActualDuration();
            }
            if (r.getAccuracy() != null) {
                totalAccuracy = totalAccuracy.add(r.getAccuracy());
                accuracyCount++;
            }
            if (r.getStarReward() != null) {
                totalStars += r.getStarReward();
            }
        }

        result.put("totalSessions", totalSessions);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        result.put("totalDuration", String.format("%d小时%d分", minutes / 60, minutes % 60));
        if (accuracyCount > 0) {
            BigDecimal avg = totalAccuracy.divide(BigDecimal.valueOf(accuracyCount), 1, RoundingMode.HALF_UP);
            result.put("avgAccuracy", avg + "%");
        } else {
            result.put("avgAccuracy", "—");
        }
        result.put("totalStars", totalStars);
        return result;
    }

    /**
     * 生成训练建议
     */
    private String generateAdvice(UserAbility ability, List<TrainingRecord> records) {
        StringBuilder sb = new StringBuilder();
        if (ability == null) {
            sb.append("建议尽快完成一次能力评估，系统会根据孩子的能力水平推荐最合适的训练内容和难度。\n");
            return sb.toString();
        }

        // 找出最薄弱项
        String[] names = {"专注时长", "视觉注意力", "听觉注意力", "工作记忆", "抑制控制"};
        BigDecimal[] values = {
                ability.getAttentionDuration(),
                ability.getVisualAttention(),
                ability.getAuditoryAttention(),
                ability.getWorkingMemory(),
                ability.getInhibitoryControl()
        };
        int weakestIdx = -1;
        BigDecimal minVal = null;
        for (int i = 0; i < values.length; i++) {
            BigDecimal v = values[i] != null ? values[i] : BigDecimal.ZERO;
            if (minVal == null || v.compareTo(minVal) < 0) {
                minVal = v;
                weakestIdx = i;
            }
        }

        if (weakestIdx >= 0) {
            sb.append("建议重点加强").append(names[weakestIdx]).append("能力的训练，");
            switch (weakestIdx) {
                case 0:
                    sb.append("可以从5分钟的专注时长训练开始，逐步增加训练时长。");
                    break;
                case 1:
                    sb.append("推荐多练习舒尔特方格和数字闪现游戏，从低难度开始逐步提升。");
                    break;
                case 2:
                    sb.append("建议多进行声音序列训练，从3个声音的短序列开始练习。");
                    break;
                case 3:
                    sb.append("卡片配对游戏是很好的记忆训练，建议从6对开始逐步增加难度。");
                    break;
                case 4:
                    sb.append("保持稳定的训练节奏，尽量减少训练中的中断次数。");
                    break;
            }
            sb.append("\n");
        }

        // 训练频率建议
        if (records != null && !records.isEmpty()) {
            Set<LocalDate> trainingDates = new HashSet<>();
            for (TrainingRecord r : records) {
                if (r.getStartTime() != null) {
                    trainingDates.add(r.getStartTime().toLocalDate());
                }
            }
            sb.append("建议保持每天训练的习惯，规律训练比偶尔高强度训练效果更好。");
        } else {
            sb.append("目前暂无训练记录，建议每天进行15-20分钟的专注力训练，循序渐进。");
        }

        return sb.toString();
    }

    private String formatDate(LocalDateTime dt) {
        if (dt == null) return "—";
        return dt.toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd"));
    }

    private String getLevelName(Integer level) {
        if (level == null) return "—";
        switch (level) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "Lv." + level;
        }
    }
}
