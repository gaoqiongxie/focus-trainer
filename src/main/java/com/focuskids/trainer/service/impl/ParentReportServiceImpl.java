package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.entity.UserAbility;
import com.focuskids.trainer.mapper.SysUserMapper;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.mapper.UserAbilityMapper;
import com.focuskids.trainer.service.ParentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 家长端数据报告服务实现
 */
@Service
@RequiredArgsConstructor
public class ParentReportServiceImpl implements ParentReportService {

    private final SysUserMapper userMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final UserAbilityMapper userAbilityMapper;

    /** 训练类型名称映射（统一维护，避免重复定义） */
    private static final Map<Integer, String> TYPE_NAMES = new HashMap<>();
    static {
        TYPE_NAMES.put(1, "专注时长");
        TYPE_NAMES.put(2, "视觉追踪");
        TYPE_NAMES.put(3, "听觉专注");
        TYPE_NAMES.put(4, "记忆训练");
        TYPE_NAMES.put(21, "数字闪现");
        TYPE_NAMES.put(41, "卡片配对");
    }

    @Override
    public Map<String, Object> getDashboard(Long parentUserId, Long childId) {
        Long targetChildId = resolveChildId(parentUserId, childId);
        LocalDate today = LocalDate.now();

        Map<String, Object> result = new HashMap<>();

        // 今日统计
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        Map<String, Object> todayStats = calcStats(targetChildId, todayStart, todayEnd);
        result.put("today", todayStats);

        // 本周统计
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        Map<String, Object> weekStats = calcStats(targetChildId, weekStart, todayEnd);
        result.put("week", weekStats);

        // 本月统计
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        Map<String, Object> monthStats = calcStats(targetChildId, monthStart, todayEnd);
        result.put("month", monthStats);

        // 最新能力评估
        LambdaQueryWrapper<UserAbility> abilityWrapper = new LambdaQueryWrapper<>();
        abilityWrapper.eq(UserAbility::getUserId, targetChildId)
                      .orderByDesc(UserAbility::getEvaluateDate)
                      .last("LIMIT 1");
        UserAbility latestAbility = userAbilityMapper.selectOne(abilityWrapper);
        if (latestAbility != null) {
            Map<String, Object> ability = new HashMap<>();
            ability.put("attentionDuration", latestAbility.getAttentionDuration());
            ability.put("visualAttention", latestAbility.getVisualAttention());
            ability.put("auditoryAttention", latestAbility.getAuditoryAttention());
            ability.put("workingMemory", latestAbility.getWorkingMemory());
            ability.put("inhibitoryControl", latestAbility.getInhibitoryControl());
            ability.put("totalScore", latestAbility.getTotalScore());
            ability.put("level", latestAbility.getAbilityLevel());
            ability.put("evaluateDate", latestAbility.getEvaluateDate());
            result.put("latestAbility", ability);
        }

        // 获取儿童信息
        SysUser child = userMapper.selectById(targetChildId);
        Map<String, Object> childInfo = new HashMap<>();
        childInfo.put("userId", targetChildId);
        childInfo.put("nickname", child != null ? child.getNickname() : "");
        childInfo.put("avatar", child != null ? (child.getAvatar() != null ? child.getAvatar() : "") : "");
        result.put("child", childInfo);

        return result;
    }

    @Override
    public List<Map<String, Object>> getTrainingTrend(Long parentUserId, Long childId, int days) {
        Long targetChildId = resolveChildId(parentUserId, childId);
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("weekday", getWeekdayName(date));

            Map<String, Object> stats = calcStats(targetChildId, dayStart, dayEnd);
            dayData.putAll(stats);

            trend.add(dayData);
        }

        return trend;
    }

    @Override
    public Map<String, Object> getAbilityAnalysis(Long parentUserId, Long childId) {
        Long targetChildId = resolveChildId(parentUserId, childId);
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        Map<String, Object> result = new HashMap<>();

        // 优先使用 UserAbility 表的最新评估数据
        List<UserAbility> abilityHistory = userAbilityMapper.selectHistoryByUserId(targetChildId, 1);
        boolean useEvaluationData = !abilityHistory.isEmpty()
                && ChronoUnit.DAYS.between(abilityHistory.get(0).getEvaluateDate(), today) <= 7;

        List<Map<String, Object>> radarData = new ArrayList<>();

        if (useEvaluationData) {
            UserAbility ua = abilityHistory.get(0);
            radarData.add(createRadarItem("专注时长", ua.getAttentionDuration() == null ? 0 : ua.getAttentionDuration().doubleValue()));
            radarData.add(createRadarItem("视觉注意力", ua.getVisualAttention() == null ? 0 : ua.getVisualAttention().doubleValue()));
            radarData.add(createRadarItem("听觉注意力", ua.getAuditoryAttention() == null ? 0 : ua.getAuditoryAttention().doubleValue()));
            radarData.add(createRadarItem("工作记忆", ua.getWorkingMemory() == null ? 0 : ua.getWorkingMemory().doubleValue()));
            radarData.add(createRadarItem("抑制控制", ua.getInhibitoryControl() == null ? 0 : ua.getInhibitoryControl().doubleValue()));
            result.put("totalScore", ua.getTotalScore() == null ? 0 : ua.getTotalScore().doubleValue());
            result.put("level", ua.getAbilityLevel());
            result.put("fromEvaluation", true);
        } else {
            // 回退：基于近7天训练数据计算
            Map<String, Object> focusScore = calcTypeAbility(targetChildId, 1, weekStart, today.atTime(LocalTime.MAX));
            Map<String, Object> visualScore = calcTypeAbility(targetChildId, 2, weekStart, today.atTime(LocalTime.MAX));
            Map<String, Object> auditoryScore = calcTypeAbility(targetChildId, 3, weekStart, today.atTime(LocalTime.MAX));
            Map<String, Object> memoryScore = calcTypeAbility(targetChildId, 4, weekStart, today.atTime(LocalTime.MAX));

            radarData.add(createRadarItem("专注时长", parseScore(focusScore.get("avgAccuracy"))));
            radarData.add(createRadarItem("视觉注意力", parseScore(visualScore.get("avgAccuracy"))));
            radarData.add(createRadarItem("听觉注意力", parseScore(auditoryScore.get("avgAccuracy"))));
            radarData.add(createRadarItem("工作记忆", parseScore(memoryScore.get("avgAccuracy"))));

            double interruptRate = calcInterruptRate(targetChildId, weekStart, today.atTime(LocalTime.MAX));
            double inhibitoryScore = Math.max(0, (1 - interruptRate) * 100);
            radarData.add(createRadarItem("抑制控制", Math.round(inhibitoryScore)));

            result.put("focus", focusScore);
            result.put("visual", visualScore);
            result.put("auditory", auditoryScore);
            result.put("memory", memoryScore);

            double totalScore = radarData.stream()
                    .mapToDouble(d -> ((Number) d.get("score")).doubleValue())
                    .average()
                    .orElse(0);
            result.put("totalScore", Math.round(totalScore * 10) / 10.0);
            result.put("level", calcAbilityLevel(totalScore));
            result.put("fromEvaluation", false);
        }

        result.put("radarData", radarData);

        // 月度对比（基于训练记录计算）
        Map<String, Object> monthFocus = calcTypeAbility(targetChildId, 1, monthStart, today.atTime(LocalTime.MAX));
        Map<String, Object> monthVisual = calcTypeAbility(targetChildId, 2, monthStart, today.atTime(LocalTime.MAX));
        Map<String, Object> monthAuditory = calcTypeAbility(targetChildId, 3, monthStart, today.atTime(LocalTime.MAX));
        Map<String, Object> monthMemory = calcTypeAbility(targetChildId, 4, monthStart, today.atTime(LocalTime.MAX));

        Map<String, Object> monthCompare = new HashMap<>();
        monthCompare.put("focus", monthFocus);
        monthCompare.put("visual", monthVisual);
        monthCompare.put("auditory", monthAuditory);
        monthCompare.put("memory", monthMemory);
        result.put("monthCompare", monthCompare);

        return result;
    }

    @Override
    public Map<String, Object> getDetailedRecords(Long parentUserId, Long childId,
                                                   Integer trainingType, int page, int size) {
        Long targetChildId = resolveChildId(parentUserId, childId);

        // 分页参数校验
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;

        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, targetChildId);
        if (trainingType != null) {
            wrapper.eq(TrainingRecord::getTrainingType, trainingType);
        }
        wrapper.orderByDesc(TrainingRecord::getStartTime);

        long total = trainingRecordMapper.selectCount(wrapper);

        // 分页查询
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);
        List<TrainingRecord> records = trainingRecordMapper.selectList(wrapper);

        // 训练类型名称映射
        Map<Integer, String> typeNames = new HashMap<>();
        typeNames.put(1, "专注时长");
        typeNames.put(2, "视觉追踪");
        typeNames.put(3, "听觉专注");
        typeNames.put(4, "记忆训练");
        typeNames.put(21, "数字闪现");
        typeNames.put(41, "卡片配对");

        List<Map<String, Object>> recordList = new ArrayList<>();
        for (TrainingRecord r : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("recordId", r.getRecordId());
            item.put("trainingType", r.getTrainingType());
            item.put("typeName", typeNames.getOrDefault(r.getTrainingType(), "其他训练"));
            item.put("level", r.getLevel());
            item.put("duration", r.getDuration());
            item.put("actualDuration", r.getActualDuration());
            item.put("status", r.getStatus());
            item.put("statusName", r.getStatus() == 1 ? "完成" : (r.getStatus() == 2 ? "中断" : "进行中"));
            item.put("accuracy", r.getAccuracy());
            item.put("score", r.getScore());
            item.put("starReward", r.getStarReward());
            item.put("startTime", r.getStartTime());
            item.put("endTime", r.getEndTime());
            recordList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", recordList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (total + size - 1) / size);

        return result;
    }

    @Override
    public Map<String, Object> getWeeklyReport(Long parentUserId, Long childId) {
        Long targetChildId = resolveChildId(parentUserId, childId);
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = today.atTime(LocalTime.MAX);

        Map<String, Object> report = new HashMap<>();
        report.put("weekStart", weekStart.toLocalDate().toString());
        report.put("weekEnd", weekEnd.toLocalDate().toString());

        // 本周统计
        Map<String, Object> stats = calcStats(targetChildId, weekStart, weekEnd);
        report.put("statistics", stats);

        // 每日明细
        List<Map<String, Object>> dailyData = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.toLocalDate().plusDays(i);
            if (date.isAfter(today)) break;

            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            Map<String, Object> dayStats = calcStats(targetChildId, dayStart, dayEnd);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("weekday", getWeekdayName(date));
            dayData.putAll(dayStats);
            dailyData.add(dayData);
        }
        report.put("dailyData", dailyData);

        // 训练类型分布
        Map<String, Object> typeDist = calcTypeDistribution(targetChildId, weekStart, weekEnd);
        report.put("typeDistribution", typeDist);

        // 亮点与建议
        List<String> highlights = generateHighlights(stats, typeDist);
        report.put("highlights", highlights);

        return report;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析目标儿童ID
     * 如果parentUserId本身是儿童，直接返回；如果是家长且指定childId，验证绑定关系后返回
     */
    public Long resolveChildId(Long parentUserId, Long childId) {
        SysUser user = userMapper.selectById(parentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 如果是儿童用户，直接返回自己的ID
        if (user.getUserType() == 1) {
            return parentUserId;
        }

        // 如果是家长用户
        if (childId != null) {
            // 验证绑定关系
            SysUser child = userMapper.selectById(childId);
            if (child != null && parentUserId.equals(child.getParentId())) {
                return childId;
            }
        }

        // 如果没有指定childId，尝试获取绑定的第一个儿童
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getParentId, parentUserId)
               .eq(SysUser::getUserType, 1)
               .eq(SysUser::getStatus, 1)
               .last("LIMIT 1");
        SysUser defaultChild = userMapper.selectOne(wrapper);
        if (defaultChild != null) {
            return defaultChild.getUserId();
        }

        // 如果没有绑定儿童，返回错误
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    /**
     * 计算指定时间段内的训练统计
     */
    private Map<String, Object> calcStats(Long userId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
               .ge(TrainingRecord::getStartTime, start)
               .le(TrainingRecord::getStartTime, end);

        List<TrainingRecord> records = trainingRecordMapper.selectList(wrapper);

        Map<String, Object> stats = new HashMap<>();
        long totalCount = records.size();
        long completedCount = records.stream().filter(r -> r.getStatus() == 1).count();
        long totalDuration = records.stream()
                .filter(r -> r.getActualDuration() != null)
                .mapToLong(TrainingRecord::getActualDuration)
                .sum();
        double avgAccuracy = records.stream()
                .filter(r -> r.getAccuracy() != null)
                .mapToDouble(r -> r.getAccuracy().doubleValue())
                .average()
                .orElse(0);
        int totalStars = records.stream()
                .filter(r -> r.getStarReward() != null)
                .mapToInt(TrainingRecord::getStarReward)
                .sum();
        int totalScore = records.stream()
                .filter(r -> r.getScore() != null)
                .mapToInt(TrainingRecord::getScore)
                .sum();
        double completionRate = totalCount > 0 ? Math.round(completedCount * 1000.0 / totalCount) / 10.0 : 0;

        stats.put("totalCount", totalCount);
        stats.put("completedCount", completedCount);
        stats.put("totalDuration", totalDuration);
        stats.put("totalDurationMinutes", Math.round(totalDuration / 60.0 * 10) / 10.0);
        stats.put("avgAccuracy", Math.round(avgAccuracy * 100) / 100.0);
        stats.put("totalStars", totalStars);
        stats.put("totalScore", totalScore);
        stats.put("completionRate", completionRate);

        return stats;
    }

    /**
     * 计算某训练类型的能力数据
     */
    private Map<String, Object> calcTypeAbility(Long userId, int trainingType, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
               .eq(TrainingRecord::getTrainingType, trainingType)
               .eq(TrainingRecord::getStatus, 1)
               .ge(TrainingRecord::getStartTime, start)
               .le(TrainingRecord::getStartTime, end);

        List<TrainingRecord> records = trainingRecordMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", records.size());

        if (records.isEmpty()) {
            result.put("avgAccuracy", 0);
            result.put("avgDuration", 0);
            result.put("bestScore", 0);
            result.put("totalStars", 0);
            return result;
        }

        double avgAccuracy = records.stream()
                .filter(r -> r.getAccuracy() != null)
                .mapToDouble(r -> r.getAccuracy().doubleValue())
                .average()
                .orElse(0);
        double avgDuration = records.stream()
                .filter(r -> r.getActualDuration() != null)
                .mapToInt(TrainingRecord::getActualDuration)
                .average()
                .orElse(0);
        int bestScore = records.stream()
                .filter(r -> r.getScore() != null)
                .mapToInt(TrainingRecord::getScore)
                .max()
                .orElse(0);
        int totalStars = records.stream()
                .filter(r -> r.getStarReward() != null)
                .mapToInt(TrainingRecord::getStarReward)
                .sum();

        result.put("avgAccuracy", Math.round(avgAccuracy * 100) / 100.0);
        result.put("avgDuration", Math.round(avgDuration * 10) / 10.0);
        result.put("bestScore", bestScore);
        result.put("totalStars", totalStars);

        return result;
    }

    /**
     * 计算中断率
     */
    private double calcInterruptRate(Long userId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
               .ge(TrainingRecord::getStartTime, start)
               .le(TrainingRecord::getStartTime, end);

        List<TrainingRecord> records = trainingRecordMapper.selectList(wrapper);
        if (records.isEmpty()) return 0;

        long interrupted = records.stream().filter(r -> r.getStatus() == 2).count();
        return (double) interrupted / records.size();
    }

    /**
     * 计算训练类型分布
     */
    private Map<String, Object> calcTypeDistribution(Long userId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
               .eq(TrainingRecord::getStatus, 1)
               .ge(TrainingRecord::getStartTime, start)
               .le(TrainingRecord::getStartTime, end);

        List<TrainingRecord> records = trainingRecordMapper.selectList(wrapper);

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (TrainingRecord r : records) {
            String name = TYPE_NAMES.getOrDefault(r.getTrainingType(), "其他");
            distribution.merge(name, 1L, Long::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("distribution", distribution);
        result.put("total", records.size());

        return result;
    }

    /**
     * 生成训练亮点和建议
     */
    private List<String> generateHighlights(Map<String, Object> stats, Map<String, Object> typeDist) {
        List<String> highlights = new ArrayList<>();

        long totalCount = ((Number) stats.getOrDefault("totalCount", 0)).longValue();
        double completionRate = ((Number) stats.getOrDefault("completionRate", 0)).doubleValue();
        double avgAccuracy = ((Number) stats.getOrDefault("avgAccuracy", 0)).doubleValue();
        int totalStars = ((Number) stats.getOrDefault("totalStars", 0)).intValue();

        if (totalCount == 0) {
            highlights.add("本周还没有训练记录，开始第一次训练吧！");
            return highlights;
        }

        if (completionRate >= 90) {
            highlights.add("🌟 本周完成率高达 " + completionRate + "%，非常棒！");
        } else if (completionRate >= 70) {
            highlights.add("👍 本周完成率 " + completionRate + "%，继续保持！");
        } else {
            highlights.add("💪 本周完成率 " + completionRate + "%，试着完成更多训练吧！");
        }

        if (avgAccuracy >= 85) {
            highlights.add("🎯 平均正确率 " + avgAccuracy + "%，注意力很集中！");
        } else if (avgAccuracy >= 60) {
            highlights.add("📊 平均正确率 " + avgAccuracy + "%，还有提升空间！");
        }

        if (totalStars >= 50) {
            highlights.add("⭐ 本周获得 " + totalStars + " 颗星星，太厉害了！");
        }

        if (totalCount >= 7) {
            highlights.add("🔥 本周训练了 " + totalCount + " 次，每天坚持很好！");
        } else {
            highlights.add("📅 本周训练了 " + totalCount + " 次，建议每天至少训练一次");
        }

        return highlights;
    }

    private double parseScore(Object value) {
        if (value == null) return 0;
        return ((Number) value).doubleValue();
    }

    private String calcAbilityLevel(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "E";
    }

    private String getWeekdayName(LocalDate date) {
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return names[date.getDayOfWeek().getValue()];
    }

    private Map<String, Object> createRadarItem(String dimension, double score) {
        Map<String, Object> item = new HashMap<>();
        item.put("dimension", dimension);
        item.put("score", score);
        return item;
    }
}
