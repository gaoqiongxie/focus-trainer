package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.entity.UserAbility;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.mapper.UserAbilityMapper;
import com.focuskids.trainer.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final UserAbilityMapper abilityMapper;
    private final TrainingRecordMapper recordMapper;

    @Override
    public void initEvaluation(Long userId) {
        // 防重：检查今天是否已有初始化记录
        LambdaQueryWrapper<UserAbility> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(UserAbility::getUserId, userId)
                   .eq(UserAbility::getEvaluateDate, LocalDate.now());
        if (abilityMapper.selectCount(checkWrapper) > 0) {
            return; // 今天已有评估记录，不重复创建
        }

        // 初始化评估记录（空值，等待训练数据填充）
        UserAbility ability = new UserAbility();
        ability.setUserId(userId);
        ability.setEvaluateDate(LocalDate.now());
        ability.setAttentionDuration(BigDecimal.ZERO);
        ability.setVisualAttention(BigDecimal.ZERO);
        ability.setAuditoryAttention(BigDecimal.ZERO);
        ability.setWorkingMemory(BigDecimal.ZERO);
        ability.setInhibitoryControl(BigDecimal.ZERO);
        ability.setTotalScore(BigDecimal.ZERO);
        ability.setAbilityLevel("E");
        abilityMapper.insert(ability);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAbility generateEvaluation(Long userId) {
        // 防重：检查今天是否已生成过评估
        LambdaQueryWrapper<UserAbility> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(UserAbility::getUserId, userId)
                   .eq(UserAbility::getEvaluateDate, LocalDate.now());
        if (abilityMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException(ErrorCode.EVALUATION_IN_PROGRESS);
        }

        LocalDateTime startTime = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime endTime = LocalDateTime.now();

        // 获取最近7天训练数据
        Map<String, Object> stats = recordMapper.selectTrainingStatistics(userId, startTime, endTime);
        if (stats == null || ((Number) stats.getOrDefault("totalCount", 0)).intValue() == 0) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }

        // 计算各维度得分
        int totalDuration = ((Number) stats.getOrDefault("totalDuration", 0)).intValue();
        double avgAccuracy = ((Number) stats.getOrDefault("avgAccuracy", 0)).doubleValue();
        int completedCount = ((Number) stats.getOrDefault("completedCount", 0)).intValue();
        int totalCount = ((Number) stats.getOrDefault("totalCount", 0)).intValue();

        // 专注时长得分：基于平均每次训练时长（满分按20分钟算）
        double avgDurationPerTrain = totalDuration > 0 && completedCount > 0
                ? (double) totalDuration / completedCount : 0;
        double attentionDurationScore = Math.min(100, avgDurationPerTrain / 1200.0 * 100);

        // 视觉注意力得分：视觉训练正确率
        double visualScore = getAccuracyByType(userId, 2, startTime, endTime);

        // 听觉注意力得分：听觉训练正确率
        double auditoryScore = getAccuracyByType(userId, 3, startTime, endTime);

        // 工作记忆得分：记忆训练正确率
        double memoryScore = getAccuracyByType(userId, 4, startTime, endTime);

        // 抑制控制得分：基于中断率
        double interruptRate = totalCount > 0
                ? (totalCount - completedCount) * 100.0 / totalCount : 100;
        double inhibitoryScore = Math.max(0, 100 - interruptRate);

        // 综合得分
        double totalScore = (attentionDurationScore + visualScore + auditoryScore
                + memoryScore + inhibitoryScore) / 5.0;

        // 确定等级
        String level;
        if (totalScore >= 90) level = "A";
        else if (totalScore >= 80) level = "B";
        else if (totalScore >= 70) level = "C";
        else if (totalScore >= 60) level = "D";
        else level = "E";

        // 保存评估
        UserAbility ability = new UserAbility();
        ability.setUserId(userId);
        ability.setAttentionDuration(BigDecimal.valueOf(attentionDurationScore).setScale(2, RoundingMode.HALF_UP));
        ability.setVisualAttention(BigDecimal.valueOf(visualScore).setScale(2, RoundingMode.HALF_UP));
        ability.setAuditoryAttention(BigDecimal.valueOf(auditoryScore).setScale(2, RoundingMode.HALF_UP));
        ability.setWorkingMemory(BigDecimal.valueOf(memoryScore).setScale(2, RoundingMode.HALF_UP));
        ability.setInhibitoryControl(BigDecimal.valueOf(inhibitoryScore).setScale(2, RoundingMode.HALF_UP));
        ability.setTotalScore(BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP));
        ability.setAbilityLevel(level);
        ability.setEvaluateDate(LocalDate.now());

        abilityMapper.insert(ability);
        return ability;
    }

    private double getAccuracyByType(Long userId, int trainingType, LocalDateTime start, LocalDateTime end) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TrainingRecord> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
               .eq(TrainingRecord::getTrainingType, trainingType)
               .eq(TrainingRecord::getStatus, 1)
               .between(TrainingRecord::getStartTime, start, end);

        List<TrainingRecord> records = recordMapper.selectList(wrapper);
        if (records.isEmpty()) return 0.0; // 无数据时给0分，不虚高

        double avg = records.stream()
                .filter(r -> r.getAccuracy() != null)
                .mapToDouble(r -> r.getAccuracy().doubleValue())
                .average().orElse(0.0);
        return Math.min(100, avg);
    }

    @Override
    public UserAbility getLatestEvaluation(Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAbility> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(UserAbility::getUserId, userId)
               .orderByDesc(UserAbility::getEvaluateDate)
               .last("LIMIT 1");
        return abilityMapper.selectOne(wrapper);
    }

    @Override
    public List<UserAbility> getEvaluationHistory(Long userId, int limit) {
        return abilityMapper.selectHistoryByUserId(userId, limit);
    }

    @Override
    public BigDecimal[] calculateAbilityScores(Long userId) {
        UserAbility ability = getLatestEvaluation(userId);
        BigDecimal[] scores = new BigDecimal[5];
        if (ability == null) {
            Arrays.fill(scores, BigDecimal.ZERO);
            return scores;
        }
        scores[0] = ability.getAttentionDuration() != null ? ability.getAttentionDuration() : BigDecimal.ZERO;
        scores[1] = ability.getVisualAttention() != null ? ability.getVisualAttention() : BigDecimal.ZERO;
        scores[2] = ability.getAuditoryAttention() != null ? ability.getAuditoryAttention() : BigDecimal.ZERO;
        scores[3] = ability.getWorkingMemory() != null ? ability.getWorkingMemory() : BigDecimal.ZERO;
        scores[4] = ability.getInhibitoryControl() != null ? ability.getInhibitoryControl() : BigDecimal.ZERO;
        return scores;
    }

    @Override
    public List<Map<String, Object>> getRecommendations(UserAbility ability) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        if (ability == null) {
            return recommendations;
        }

        // 5维：专注时长, 视觉注意力, 听觉注意力, 工作记忆, 抑制控制
        String[] names = {"专注时长", "视觉注意力", "听觉注意力", "工作记忆", "抑制控制"};
        BigDecimal[] values = {
                ability.getAttentionDuration(),
                ability.getVisualAttention(),
                ability.getAuditoryAttention(),
                ability.getWorkingMemory(),
                ability.getInhibitoryControl()
        };
        int[] recommendTypes = {1, 2, 3, 4, 1};

        // 找最薄弱项
        BigDecimal minVal = null;
        int minIdx = -1;
        for (int i = 0; i < values.length; i++) {
            BigDecimal v = values[i] != null ? values[i] : BigDecimal.ZERO;
            if (minVal == null || v.compareTo(minVal) < 0) {
                minVal = v;
                minIdx = i;
            }
        }

        if (minIdx >= 0) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("dimension", names[minIdx]);
            rec.put("score", minVal);
            rec.put("recommendTrainingType", recommendTypes[minIdx]);
            rec.put("reason", names[minIdx] + "能力相对薄弱，建议重点训练");
            recommendations.add(rec);
        }

        return recommendations;
    }
}
