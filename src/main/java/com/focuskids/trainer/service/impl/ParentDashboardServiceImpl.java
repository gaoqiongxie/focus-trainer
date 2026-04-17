package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.entity.*;
import com.focuskids.trainer.mapper.*;
import com.focuskids.trainer.service.ParentDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentDashboardServiceImpl implements ParentDashboardService {

    private final SysUserMapper sysUserMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final UserAbilityMapper userAbilityMapper;
    private final UserStreakMapper userStreakMapper;
    private final DailyTaskMapper dailyTaskMapper;

    @Override
    public List<Map<String, Object>> getChildrenSummary(Long parentId) {
        // 获取所有关联孩子
        List<SysUser> children = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getParentId, parentId)
                        .eq(SysUser::getStatus, 1)
        );

        List<Map<String, Object>> summaryList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (SysUser child : children) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("userId", child.getUserId());
            summary.put("nickname", child.getNickname());
            summary.put("avatar", child.getAvatar());
            summary.put("grade", child.getGrade());
            summary.put("starCount", child.getStarCount());

            // 今日训练
            List<TrainingRecord> todayRecords = trainingRecordMapper.selectList(
                    new LambdaQueryWrapper<TrainingRecord>()
                            .eq(TrainingRecord::getUserId, child.getUserId())
                            .eq(TrainingRecord::getStatus, 1)
                            .ge(TrainingRecord::getEndTime, today.atStartOfDay())
                            .lt(TrainingRecord::getEndTime, today.plusDays(1).atStartOfDay())
            );
            summary.put("todaySessions", todayRecords.size());
            summary.put("todayMinutes", todayRecords.stream()
                    .mapToInt(r -> r.getActualDuration() != null ? r.getActualDuration() : 0)
                    .sum() / 60);

            // 连续打卡
            UserStreak streak = userStreakMapper.selectOne(
                    new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, child.getUserId())
            );
            summary.put("currentStreak", streak != null ? streak.getCurrentStreak() : 0);
            summary.put("maxStreak", streak != null ? streak.getMaxStreak() : 0);

            // 最新能力评估
            UserAbility ability = userAbilityMapper.selectOne(
                    new LambdaQueryWrapper<UserAbility>()
                            .eq(UserAbility::getUserId, child.getUserId())
                            .orderByDesc(UserAbility::getEvaluateDate)
                            .last("LIMIT 1")
            );
            if (ability != null) {
                summary.put("abilityLevel", ability.getAbilityLevel());
                summary.put("totalScore", ability.getTotalScore());
            } else {
                summary.put("abilityLevel", null);
                summary.put("totalScore", null);
            }

            summaryList.add(summary);
        }

        return summaryList;
    }

    @Override
    public Map<String, Object> getTrendAnalysis(Long userId, int weeks) {
        if (weeks <= 0) weeks = 4;
        if (weeks > 12) weeks = 12;

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(weeks);

        // 获取指定范围内的完成记录
        List<TrainingRecord> records = trainingRecordMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getUserId, userId)
                        .eq(TrainingRecord::getStatus, 1)
                        .ge(TrainingRecord::getEndTime, startDate.atStartOfDay())
                        .le(TrainingRecord::getEndTime, endDate.plusDays(1).atStartOfDay())
        );

        // 按周分组
        WeekFields weekFields = WeekFields.ISO;
        Map<Integer, List<TrainingRecord>> byWeek = new TreeMap<>();
        for (TrainingRecord r : records) {
            int weekNum = r.getEndTime().get(weekFields.weekOfWeekBasedYear());
            int year = r.getEndTime().getYear();
            int key = year * 100 + weekNum;
            byWeek.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        // 按周统计
        List<String> weekLabels = new ArrayList<>();
        List<Integer> sessionCounts = new ArrayList<>();
        List<Integer> minuteCounts = new ArrayList<>();
        List<Double> accuracyTrends = new ArrayList<>();

        for (Map.Entry<Integer, List<TrainingRecord>> entry : byWeek.entrySet()) {
            int year = entry.getKey() / 100;
            int weekNum = entry.getKey() % 100;
            weekLabels.add("W" + weekNum);

            List<TrainingRecord> weekRecords = entry.getValue();
            sessionCounts.add(weekRecords.size());
            minuteCounts.add(weekRecords.stream()
                    .mapToInt(r -> r.getActualDuration() != null ? r.getActualDuration() : 0)
                    .sum() / 60);
            accuracyTrends.add(weekRecords.stream()
                    .filter(r -> r.getAccuracy() != null)
                    .mapToDouble(r -> r.getAccuracy().doubleValue())
                    .average()
                    .orElse(0));
        }

        // 获取能力变化趋势
        List<UserAbility> abilities = userAbilityMapper.selectList(
                new LambdaQueryWrapper<UserAbility>()
                        .eq(UserAbility::getUserId, userId)
                        .ge(UserAbility::getEvaluateDate, startDate)
                        .le(UserAbility::getEvaluateDate, endDate)
                        .orderByAsc(UserAbility::getEvaluateDate)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("weeks", weeks);
        result.put("weekLabels", weekLabels);
        result.put("sessionCounts", sessionCounts);
        result.put("minuteCounts", minuteCounts);
        result.put("accuracyTrends", accuracyTrends);
        result.put("abilityTrends", abilities);

        // 总结
        if (!records.isEmpty()) {
            int totalSessions = records.size();
            int totalMinutes = minuteCounts.stream().mapToInt(Integer::intValue).sum();
            double avgAccuracy = accuracyTrends.stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);

            // 趋势判断（比较前半段 vs 后半段）
            int half = accuracyTrends.size() / 2;
            double firstHalf = accuracyTrends.subList(0, Math.max(half, 1)).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            double secondHalf = accuracyTrends.subList(half, accuracyTrends.size()).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            String trend;
            if (secondHalf - firstHalf > 5) {
                trend = "improving";
            } else if (firstHalf - secondHalf > 5) {
                trend = "declining";
            } else {
                trend = "stable";
            }

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalSessions", totalSessions);
            summary.put("totalMinutes", totalMinutes);
            summary.put("avgAccuracy", BigDecimal.valueOf(avgAccuracy).setScale(1, RoundingMode.HALF_UP));
            summary.put("trend", trend);
            result.put("summary", summary);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getExpertAdvice(Long userId) {
        List<Map<String, Object>> adviceList = new ArrayList<>();

        // 获取最新能力评估
        UserAbility ability = userAbilityMapper.selectOne(
                new LambdaQueryWrapper<UserAbility>()
                        .eq(UserAbility::getUserId, userId)
                        .orderByDesc(UserAbility::getEvaluateDate)
                        .last("LIMIT 1")
        );

        SysUser user = sysUserMapper.selectById(userId);
        String nickname = user != null ? user.getNickname() : "孩子";

        if (ability == null) {
            Map<String, Object> advice = new LinkedHashMap<>();
            advice.put("type", "general");
            advice.put("title", "开始训练吧");
            advice.put("content", nickname + "还没有完成足够的训练来生成能力评估，建议每天至少完成1-2次训练。");
            advice.put("priority", 1);
            adviceList.add(advice);
            return adviceList;
        }

        // 识别薄弱项（<60分）
        String[] dimensions = {"attentionDuration", "visualAttention", "auditoryAttention",
                "workingMemory", "inhibitoryControl"};
        String[] dimNames = {"专注时长", "视觉注意力", "听觉注意力", "工作记忆", "抑制控制"};
        String[] trainSuggestions = {
                "专注时长训练（每天5-15分钟）",
                "舒尔特方格/数字闪现游戏",
                "声音序列记忆游戏",
                "卡片配对记忆游戏",
                "专注时长训练（减少中断次数）"
        };

        int priority = 1;
        for (int i = 0; i < dimensions.length; i++) {
            BigDecimal score = getScoreByDimension(ability, dimensions[i]);
            if (score != null && score.compareTo(new BigDecimal("60")) < 0) {
                Map<String, Object> advice = new LinkedHashMap<>();
                advice.put("type", "weakness");
                advice.put("dimension", dimNames[i]);
                advice.put("title", dimNames[i] + "需要加强");
                advice.put("content", nickname + "的" + dimNames[i] + "得分为" + score
                        + "分，低于及格线。建议多做" + trainSuggestions[i] + "。");
                advice.put("score", score);
                advice.put("priority", priority++);
                adviceList.add(advice);
            }
        }

        // 训练频率建议
        UserStreak streak = userStreakMapper.selectOne(
                new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, userId)
        );
        if (streak == null || streak.getCurrentStreak() < 3) {
            Map<String, Object> advice = new LinkedHashMap<>();
            advice.put("type", "streak");
            advice.put("title", "培养训练习惯");
            advice.put("content", "坚持每天训练可以帮助" + nickname + "养成良好习惯。"
                    + (streak != null ? "目前已连续" + streak.getCurrentStreak() + "天，" : "")
                    + "目标：连续3天以上。");
            advice.put("priority", priority++);
            adviceList.add(advice);
        }

        // 综合表现
        if (ability.getTotalScore() != null && ability.getTotalScore().compareTo(new BigDecimal("80")) >= 0) {
            Map<String, Object> advice = new LinkedHashMap<>();
            advice.put("type", "praise");
            advice.put("title", "表现优秀！");
            advice.put("content", nickname + "的综合能力评分已达到" + ability.getTotalScore()
                    + "分，继续保持！可以尝试更高难度的训练挑战自我。");
            advice.put("priority", priority);
            adviceList.add(advice);
        }

        return adviceList;
    }

    private BigDecimal getScoreByDimension(UserAbility ability, String dimension) {
        switch (dimension) {
            case "attentionDuration": return ability.getAttentionDuration();
            case "visualAttention": return ability.getVisualAttention();
            case "auditoryAttention": return ability.getAuditoryAttention();
            case "workingMemory": return ability.getWorkingMemory();
            case "inhibitoryControl": return ability.getInhibitoryControl();
            default: return null;
        }
    }
}
