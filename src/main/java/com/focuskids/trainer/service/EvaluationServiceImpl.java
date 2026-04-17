package com.focuskids.trainer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.entity.UserAbility;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.mapper.UserAbilityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 能力评估服务实现
 * 5大维度计算：
 * - 专注时长（attentionDuration）: training_type=1 平均完成率
 * - 视觉注意力（visualAttention）: training_type=2/21 平均正确率
 * - 听觉注意力（auditoryAttention）: training_type=3 平均正确率
 * - 工作记忆（workingMemory）: training_type=4/21 平均得分
 * - 抑制控制（inhibitoryControl）: 基于中断率和错误率
 * 等级: A(>=90) B(>=75) C(>=60) D(>=40) E(<40)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final UserAbilityMapper userAbilityMapper;
    private final TrainingRecordMapper trainingRecordMapper;

    @Override
    public void initEvaluation(Long userId) {
        // 首次初始化时，如果已有训练数据则直接生成评估
        BigDecimal[] scores = calculateAbilityScores(userId);
        UserAbility latest = getLatestEvaluation(userId);
        if (latest == null) {
            UserAbility evaluation = buildEvaluation(userId, scores);
            userAbilityMapper.insert(evaluation);
            log.info("用户 {} 初始化能力评估: level={}", userId, evaluation.getAbilityLevel());
        }
    }

    @Override
    public UserAbility generateEvaluation(Long userId) {
        BigDecimal[] scores = calculateAbilityScores(userId);
        UserAbility evaluation = buildEvaluation(userId, scores);

        // 查找最新记录，如有则更新，否则新增
        UserAbility latest = getLatestEvaluation(userId);
        if (latest != null && ChronoUnit.DAYS.between(latest.getEvaluateDate(), LocalDate.now()) < 7) {
            // 7天内不重复生成，更新维度得分
            latest.setAttentionDuration(scores[0]);
            latest.setVisualAttention(scores[1]);
            latest.setAuditoryAttention(scores[2]);
            latest.setWorkingMemory(scores[3]);
            latest.setInhibitoryControl(scores[4]);
            latest.setTotalScore(scores[5]);
            latest.setAbilityLevel(evaluation.getAbilityLevel());
            userAbilityMapper.updateById(latest);
            log.info("用户 {} 更新能力评估(7天内): level={}", userId, latest.getAbilityLevel());
            return latest;
        } else {
            userAbilityMapper.insert(evaluation);
            log.info("用户 {} 生成新能力评估: level={}", userId, evaluation.getAbilityLevel());
            return evaluation;
        }
    }

    @Override
    public UserAbility getLatestEvaluation(Long userId) {
        List<UserAbility> history = userAbilityMapper.selectHistoryByUserId(userId, 1);
        return history.isEmpty() ? null : history.get(0);
    }

    @Override
    public List<UserAbility> getEvaluationHistory(Long userId, int limit) {
        return userAbilityMapper.selectHistoryByUserId(userId, limit);
    }

    /**
     * 计算5个维度得分 + 综合得分
     * 返回数组: [attentionDuration, visualAttention, auditoryAttention, workingMemory, inhibitoryControl, totalScore]
     */
    @Override
    public BigDecimal[] calculateAbilityScores(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
                .eq(TrainingRecord::getStatus, 1) // 只统计完成的
                .ge(TrainingRecord::getStartTime, since);

        List<TrainingRecord> records = trainingRecordMapper.selectList(wrapper);

        BigDecimal attentionDuration = calcAttentionDuration(records);
        BigDecimal visualAttention = calcVisualAttention(records);
        BigDecimal auditoryAttention = calcAuditoryAttention(records);
        BigDecimal workingMemory = calcWorkingMemory(records);
        BigDecimal inhibitoryControl = calcInhibitoryControl(records);

        // 综合得分 = 各维度加权平均
        // 专注时长20% + 视觉注意力25% + 听觉注意力20% + 工作记忆20% + 抑制控制15%
        BigDecimal total = attentionDuration.multiply(BigDecimal.valueOf(0.20))
                .add(visualAttention.multiply(BigDecimal.valueOf(0.25)))
                .add(auditoryAttention.multiply(BigDecimal.valueOf(0.20)))
                .add(workingMemory.multiply(BigDecimal.valueOf(0.20)))
                .add(inhibitoryControl.multiply(BigDecimal.valueOf(0.15)))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("用户 {} 能力得分: 专注={}, 视觉={}, 听觉={}, 记忆={}, 抑制={}, 综合={}",
                userId, attentionDuration, visualAttention, auditoryAttention,
                workingMemory, inhibitoryControl, total);

        return new BigDecimal[]{attentionDuration, visualAttention, auditoryAttention,
                workingMemory, inhibitoryControl, total};
    }

    // 专注时长：training_type=1，actual_duration / duration * 100
    private BigDecimal calcAttentionDuration(List<TrainingRecord> records) {
        return records.stream()
                .filter(r -> r.getTrainingType() == 1)
                .filter(r -> r.getDuration() != null && r.getDuration() > 0)
                .map(r -> {
                    double rate = (double) r.getActualDuration() / r.getDuration();
                    return BigDecimal.valueOf(Math.min(rate * 100, 100));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, records.stream().filter(r -> r.getTrainingType() == 1).count())), 2, RoundingMode.HALF_UP);
    }

    // 视觉注意力：training_type=2(舒尔特) + 21(数字闪现)，取 accuracy
    private BigDecimal calcVisualAttention(List<TrainingRecord> records) {
        return records.stream()
                .filter(r -> r.getTrainingType() == 2 || r.getTrainingType() == 21)
                .filter(r -> r.getAccuracy() != null)
                .map(TrainingRecord::getAccuracy)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, records.stream().filter(r -> (r.getTrainingType() == 2 || r.getTrainingType() == 21) && r.getAccuracy() != null).count())), 2, RoundingMode.HALF_UP);
    }

    // 听觉注意力：training_type=3(声音序列)
    private BigDecimal calcAuditoryAttention(List<TrainingRecord> records) {
        return records.stream()
                .filter(r -> r.getTrainingType() == 3)
                .filter(r -> r.getAccuracy() != null)
                .map(TrainingRecord::getAccuracy)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, records.stream().filter(r -> r.getTrainingType() == 3 && r.getAccuracy() != null).count())), 2, RoundingMode.HALF_UP);
    }

    // 工作记忆：training_type=4(卡片配对) + 21(数字闪现)，基于得分归一化
    private BigDecimal calcWorkingMemory(List<TrainingRecord> records) {
        long count = records.stream()
                .filter(r -> r.getTrainingType() == 4 || r.getTrainingType() == 21)
                .count();
        if (count == 0) return BigDecimal.ZERO;
        double avgScore = records.stream()
                .filter(r -> r.getTrainingType() == 4 || r.getTrainingType() == 21)
                .filter(r -> r.getScore() != null)
                .mapToInt(TrainingRecord::getScore)
                .average()
                .orElse(0);
        // 得分 500 满分映射到 100
        return BigDecimal.valueOf(Math.min(avgScore / 500 * 100, 100)).setScale(2, RoundingMode.HALF_UP);
    }

    // 抑制控制：中断率越低越好，正确率/完成度越高越好
    private BigDecimal calcInhibitoryControl(List<TrainingRecord> records) {
        if (records.isEmpty()) return BigDecimal.ZERO;
        double avgAccuracy = records.stream()
                .filter(r -> r.getAccuracy() != null)
                .mapToDouble(r -> r.getAccuracy().doubleValue())
                .average()
                .orElse(0);
        double avgInterrupt = records.stream()
                .filter(r -> r.getInterruptCount() != null)
                .mapToInt(TrainingRecord::getInterruptCount)
                .average()
                .orElse(0);
        // 正确率权重60%，中断率权重40%（中断越少分越高）
        double score = avgAccuracy * 0.6 + Math.max(0, (10 - avgInterrupt) / 10 * 100) * 0.4;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private UserAbility buildEvaluation(Long userId, BigDecimal[] scores) {
        UserAbility evaluation = new UserAbility();
        evaluation.setUserId(userId);
        evaluation.setAttentionDuration(scores[0]);
        evaluation.setVisualAttention(scores[1]);
        evaluation.setAuditoryAttention(scores[2]);
        evaluation.setWorkingMemory(scores[3]);
        evaluation.setInhibitoryControl(scores[4]);
        evaluation.setTotalScore(scores[5]);
        evaluation.setAbilityLevel(calcLevel(scores[5]));
        evaluation.setEvaluateDate(LocalDate.now());
        evaluation.setCreateTime(LocalDateTime.now());
        return evaluation;
    }

    // A>=90 B>=75 C>=60 D>=40 E<40
    private String calcLevel(BigDecimal totalScore) {
        if (totalScore == null) return "E";
        double score = totalScore.doubleValue();
        if (score >= 90) return "A";
        if (score >= 75) return "B";
        if (score >= 60) return "C";
        if (score >= 40) return "D";
        return "E";
    }

    /**
     * 根据能力评估推荐薄弱项训练
     * 返回格式: [{ dimension, score, trainingType, trainingName, reason }]
     */
    @Override
    public List<Map<String, Object>> getRecommendations(UserAbility ability) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        if (ability == null) {
            // 无评估数据，返回默认推荐
            Map<String, Object> defaultRec = new HashMap<>();
            defaultRec.put("dimension", "综合");
            defaultRec.put("score", 0);
            defaultRec.put("trainingType", 1);
            defaultRec.put("trainingName", "专注时长训练");
            defaultRec.put("reason", "开始你的第一次训练，系统将为你生成能力报告");
            defaultRec.put("priority", 1);
            recommendations.add(defaultRec);
            return recommendations;
        }

        // 按得分从低到高排序，找出薄弱项
        List<Map.Entry<String, BigDecimal>> dimensionScores = new ArrayList<>();
        dimensionScores.add(new AbstractMap.SimpleEntry<>("专注时长", ability.getAttentionDuration()));
        dimensionScores.add(new AbstractMap.SimpleEntry<>("视觉注意力", ability.getVisualAttention()));
        dimensionScores.add(new AbstractMap.SimpleEntry<>("听觉注意力", ability.getAuditoryAttention()));
        dimensionScores.add(new AbstractMap.SimpleEntry<>("工作记忆", ability.getWorkingMemory()));
        dimensionScores.add(new AbstractMap.SimpleEntry<>("抑制控制", ability.getInhibitoryControl()));
        dimensionScores.sort(Comparator.comparing(e -> e.getValue() == null ? BigDecimal.ZERO : e.getValue()));

        // 映射到训练类型
        Map<String, Map<String, Object>> dimensionToTraining = new LinkedHashMap<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("trainingType", 1);
        map1.put("trainingName", "专注时长训练");
        map1.put("level", 1);
        dimensionToTraining.put("专注时长", map1);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("trainingType", 2);
        map2.put("trainingName", "舒尔特方格");
        map2.put("level", 1);
        dimensionToTraining.put("视觉注意力", map2);
        Map<String, Object> map3 = new HashMap<>();
        map3.put("trainingType", 3);
        map3.put("trainingName", "声音序列");
        map3.put("level", 1);
        dimensionToTraining.put("听觉注意力", map3);
        Map<String, Object> map4 = new HashMap<>();
        map4.put("trainingType", 4);
        map4.put("trainingName", "卡片配对");
        map4.put("level", 1);
        dimensionToTraining.put("工作记忆", map4);
        Map<String, Object> map5 = new HashMap<>();
        map5.put("trainingType", 21);
        map5.put("trainingName", "数字闪现");
        map5.put("level", 1);
        dimensionToTraining.put("抑制控制", map5);

        int priority = 1;
        for (Map.Entry<String, BigDecimal> entry : dimensionScores) {
            String dimension = entry.getKey();
            BigDecimal score = entry.getValue();
            if (score == null) score = BigDecimal.ZERO;

            Map<String, Object> training = dimensionToTraining.get(dimension);
            if (training == null) continue;

            String reason = score.doubleValue() < 60
                    ? "薄弱项，建议优先训练"
                    : score.doubleValue() < 75
                            ? "待提升，可以加强练习"
                            : "表现良好，建议保持";

            Map<String, Object> rec = new HashMap<>();
            rec.put("dimension", dimension);
            rec.put("score", score);
            rec.put("trainingType", training.get("trainingType"));
            rec.put("trainingName", training.get("trainingName"));
            rec.put("level", training.get("level"));
            rec.put("reason", reason);
            rec.put("priority", priority++);
            recommendations.add(rec);
        }

        return recommendations;
    }
}
