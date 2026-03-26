package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.entity.TrainingConfig;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.mapper.TrainingConfigMapper;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.service.RewardService;
import com.focuskids.trainer.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    private final TrainingConfigMapper configMapper;
    private final TrainingRecordMapper recordMapper;
    private final RewardService rewardService;

    @Override
    public List<TrainingConfig> getConfigList(Integer trainingType) {
        if (trainingType != null) {
            return configMapper.selectActiveByType(trainingType);
        }
        LambdaQueryWrapper<TrainingConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingConfig::getIsActive, 1).orderByAsc(TrainingConfig::getTrainingType, TrainingConfig::getLevel);
        return configMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public TrainingRecord startTraining(Long userId, Integer trainingType, Integer level, Integer duration) {
        // 检查是否有进行中的训练
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId)
               .eq(TrainingRecord::getStatus, 0);
        if (recordMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.TRAINING_IN_PROGRESS);
        }

        // 检查训练配置
        LambdaQueryWrapper<TrainingConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(TrainingConfig::getTrainingType, trainingType)
                     .eq(TrainingConfig::getLevel, level)
                     .eq(TrainingConfig::getIsActive, 1);
        TrainingConfig config = configMapper.selectOne(configWrapper);
        if (config == null) {
            throw new BusinessException(ErrorCode.TRAINING_CONFIG_ERROR);
        }

        TrainingRecord record = new TrainingRecord();
        record.setUserId(userId);
        record.setTrainingType(trainingType);
        record.setLevel(level);
        record.setDuration(duration != null ? duration : config.getDuration());
        record.setActualDuration(0);
        record.setStatus(0);
        record.setInterruptCount(0);
        record.setStartTime(LocalDateTime.now());
        recordMapper.insert(record);

        return record;
    }

    @Override
    @Transactional
    public TrainingRecord completeTraining(Long recordId, Integer actualDuration, Integer interruptCount,
                                            Double accuracy, Integer score) {
        TrainingRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.TRAINING_NOT_FOUND);
        }

        record.setActualDuration(actualDuration);
        record.setInterruptCount(interruptCount);
        record.setAccuracy(BigDecimal.valueOf(accuracy).setScale(2, RoundingMode.HALF_UP));
        record.setScore(score);
        record.setStatus(1);
        record.setEndTime(LocalDateTime.now());

        // 计算星星奖励：基础分 + 正确率奖励
        int starReward = (int) Math.round(actualDuration / 60.0 * 2);
        if (accuracy != null && accuracy >= 80) {
            starReward += 5;
        } else if (accuracy != null && accuracy >= 60) {
            starReward += 2;
        }
        record.setStarReward(starReward);

        recordMapper.updateById(record);

        // 发放星星奖励
        if (starReward > 0) {
            rewardService.addStars(record.getUserId(), starReward, 1, recordId);
        }

        return record;
    }

    @Override
    @Transactional
    public void interruptTraining(Long recordId) {
        TrainingRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.TRAINING_NOT_FOUND);
        }

        record.setStatus(2);
        record.setEndTime(LocalDateTime.now());
        if (record.getActualDuration() == null || record.getActualDuration() == 0) {
            record.setActualDuration((int) java.time.Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds());
        }
        recordMapper.updateById(record);
    }

    @Override
    public Map<String, Object> getStatistics(Long userId, String period) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime;
        switch (period != null ? period : "week") {
            case "day":
                startTime = endTime.toLocalDate().atStartOfDay();
                break;
            case "month":
                startTime = endTime.minusMonths(1);
                break;
            default:
                startTime = endTime.minusWeeks(1);
        }

        Map<String, Object> stats = recordMapper.selectTrainingStatistics(userId, startTime, endTime);
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("totalCount", 0);
            stats.put("completedCount", 0);
            stats.put("totalDuration", 0);
            stats.put("avgAccuracy", 0);
            stats.put("totalStars", 0);
        }

        // 计算完成率
        int total = ((Number) stats.getOrDefault("totalCount", 0)).intValue();
        int completed = ((Number) stats.getOrDefault("completedCount", 0)).intValue();
        double completionRate = total > 0 ? Math.round(completed * 100.0 / total * 10) / 10.0 : 0;
        stats.put("completionRate", completionRate);
        stats.put("period", period);

        return stats;
    }

    @Override
    public List<TrainingRecord> getRecords(Long userId, Integer trainingType, int page, int size) {
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getUserId, userId);
        if (trainingType != null) {
            wrapper.eq(TrainingRecord::getTrainingType, trainingType);
        }
        wrapper.orderByDesc(TrainingRecord::getStartTime);

        Page<TrainingRecord> pageParam = new Page<>(page, size);
        return recordMapper.selectPage(pageParam, wrapper).getRecords();
    }
}
