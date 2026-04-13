package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.entity.TrainingConfig;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.entity.UserAbility;
import com.focuskids.trainer.mapper.TrainingConfigMapper;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.mapper.UserAbilityMapper;
import com.focuskids.trainer.service.DifficultyRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DifficultyRecommendationServiceImpl implements DifficultyRecommendationService {

    private final TrainingConfigMapper trainingConfigMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final UserAbilityMapper userAbilityMapper;

    @Override
    public List<Map<String, Object>> getRecommendations(Long userId) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // 获取所有启用的训练类型
        List<TrainingConfig> allConfigs = trainingConfigMapper.selectList(
                new LambdaQueryWrapper<TrainingConfig>()
                        .eq(TrainingConfig::getIsActive, 1)
                        .orderByAsc(TrainingConfig::getTrainingType, TrainingConfig::getLevel)
        );

        // 按训练类型分组
        Map<Integer, List<TrainingConfig>> configsByType = allConfigs.stream()
                .collect(Collectors.groupingBy(TrainingConfig::getTrainingType));

        for (Map.Entry<Integer, List<TrainingConfig>> entry : configsByType.entrySet()) {
            Integer trainingType = entry.getKey();
            Map<String, Object> rec = recommendForType(userId, trainingType);
            if (rec != null) {
                recommendations.add(rec);
            }
        }

        return recommendations;
    }

    @Override
    public Map<String, Object> recommendForType(Long userId, Integer trainingType) {
        List<TrainingConfig> configs = trainingConfigMapper.selectList(
                new LambdaQueryWrapper<TrainingConfig>()
                        .eq(TrainingConfig::getTrainingType, trainingType)
                        .eq(TrainingConfig::getIsActive, 1)
                        .orderByAsc(TrainingConfig::getLevel)
        );

        if (configs.isEmpty()) {
            return null;
        }

        // 获取用户最近30天的完成记录
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<TrainingRecord> recentRecords = trainingRecordMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getUserId, userId)
                        .eq(TrainingRecord::getTrainingType, trainingType)
                        .eq(TrainingRecord::getStatus, 1)
                        .ge(TrainingRecord::getEndTime, thirtyDaysAgo)
        );

        // 新用户：推荐最低难度
        if (recentRecords.isEmpty()) {
            TrainingConfig config = configs.get(0);
            Map<String, Object> rec = buildRecommendation(config);
            rec.put("reason", "欢迎新挑战！从入门难度开始吧");
            return rec;
        }

        // 计算各难度通过率
        Map<Integer, List<TrainingRecord>> recordsByLevel = recentRecords.stream()
                .collect(Collectors.groupingBy(TrainingRecord::getLevel));

        Map<Integer, BigDecimal> passRates = new HashMap<>();
        for (Map.Entry<Integer, List<TrainingRecord>> levelEntry : recordsByLevel.entrySet()) {
            Integer level = levelEntry.getKey();
            List<TrainingRecord> records = levelEntry.getValue();
            // 通过率定义：正确率>=60% 或 得分>0 视为通过
            long passCount = records.stream()
                    .filter(r -> r.getAccuracy() != null && r.getAccuracy().compareTo(new BigDecimal("60")) >= 0)
                    .count();
            BigDecimal rate = BigDecimal.valueOf(passCount)
                    .divide(BigDecimal.valueOf(records.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            passRates.put(level, rate);
        }

        // 找到舒适区（通过率70-85%的难度）
        Integer recommendedLevel = null;
        String reason;

        // 找最高通过率>=70%的难度
        Integer highestComfortable = null;
        BigDecimal highestRate = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> rateEntry : passRates.entrySet()) {
            BigDecimal rate = rateEntry.getValue();
            if (rate.compareTo(new BigDecimal("70")) >= 0
                    && rate.compareTo(new BigDecimal("85")) <= 0
                    && rate.compareTo(highestRate) >= 0) {
                highestRate = rate;
                highestComfortable = rateEntry.getKey();
            }
        }

        if (highestComfortable != null) {
            recommendedLevel = highestComfortable;
            reason = "当前难度通过率" + highestRate.intValue() + "%，适合继续挑战";
        } else {
            // 如果所有难度通过率>85%，推荐下一级难度
            Integer maxLevel = passRates.keySet().stream().max(Integer::compareTo).orElse(configs.get(0).getLevel());
            BigDecimal maxRate = passRates.getOrDefault(maxLevel, BigDecimal.ZERO);
            if (maxRate.compareTo(new BigDecimal("85")) > 0) {
                // 尝试找更高一级的配置
                recommendedLevel = configs.stream()
                        .map(TrainingConfig::getLevel)
                        .filter(l -> l > maxLevel)
                        .min(Integer::compareTo)
                        .orElse(maxLevel);
                reason = "当前难度已掌握(通过率" + maxRate.intValue() + "%)，试试更高难度！";
            } else {
                // 通过率<70%，降低难度或保持
                recommendedLevel = passRates.entrySet().stream()
                        .filter(e -> e.getValue().compareTo(new BigDecimal("50")) >= 0)
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(configs.get(0).getLevel());
                reason = "正在适应难度，继续加油练习！";
            }
        }

        // 找到对应的配置
        final Integer finalLevel = recommendedLevel;
        TrainingConfig config = configs.stream()
                .filter(c -> c.getLevel().equals(finalLevel))
                .findFirst()
                .orElse(configs.get(0));

        Map<String, Object> rec = buildRecommendation(config);
        rec.put("reason", reason);
        rec.put("passRateInfo", passRates);
        return rec;
    }

    private Map<String, Object> buildRecommendation(TrainingConfig config) {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("configId", config.getConfigId());
        rec.put("trainingType", config.getTrainingType());
        rec.put("trainingName", config.getTrainingName());
        rec.put("level", config.getLevel());
        rec.put("category", config.getCategory());
        rec.put("duration", config.getDuration());
        rec.put("description", config.getDescription());
        return rec;
    }
}
