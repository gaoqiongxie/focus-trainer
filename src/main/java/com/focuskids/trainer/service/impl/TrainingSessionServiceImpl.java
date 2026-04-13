package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.entity.ParentSetting;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.entity.TrainingSession;
import com.focuskids.trainer.mapper.ParentSettingMapper;
import com.focuskids.trainer.mapper.SysUserMapper;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.mapper.TrainingSessionMapper;
import com.focuskids.trainer.service.TrainingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingSessionServiceImpl implements TrainingSessionService {

    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final ParentSettingMapper parentSettingMapper;

    @Override
    public TrainingSession createSession(Long userId, Long recordId, String sessionData) {
        // 校验训练记录归属
        TrainingRecord record = trainingRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("训练记录不存在或无权操作");
        }

        TrainingSession session = new TrainingSession();
        session.setUserId(userId);
        session.setRecordId(recordId);
        session.setSessionData(sessionData);
        session.setStatus(1);
        session.setPauseCount(0);
        trainingSessionMapper.insert(session);
        return session;
    }

    @Override
    public void updateSessionData(Long sessionId, Long userId, String sessionData) {
        TrainingSession session = getAndValidate(sessionId, userId);
        session.setSessionData(sessionData);
        trainingSessionMapper.updateById(session);
    }

    @Override
    public TrainingSession pauseSession(Long sessionId, Long userId) {
        TrainingSession session = getAndValidate(sessionId, userId);
        if (session.getStatus() != 1) {
            throw new BusinessException("只能暂停进行中的训练");
        }
        session.setStatus(2);
        session.setPauseCount(session.getPauseCount() + 1);
        session.setLastPauseAt(LocalDateTime.now());
        trainingSessionMapper.updateById(session);
        return session;
    }

    @Override
    public TrainingSession resumeSession(Long sessionId, Long userId) {
        TrainingSession session = getAndValidate(sessionId, userId);
        if (session.getStatus() != 2) {
            throw new BusinessException("只能恢复已暂停的训练");
        }
        session.setStatus(1);
        trainingSessionMapper.updateById(session);
        return session;
    }

    @Override
    public void endSession(Long sessionId, Long userId) {
        TrainingSession session = getAndValidate(sessionId, userId);
        session.setStatus(0);
        trainingSessionMapper.updateById(session);
    }

    @Override
    public TrainingSession getActiveSession(Long userId) {
        return trainingSessionMapper.selectOne(
                new LambdaQueryWrapper<TrainingSession>()
                        .eq(TrainingSession::getUserId, userId)
                        .in(TrainingSession::getStatus, 1, 2)
                        .orderByDesc(TrainingSession::getCreateTime)
                        .last("LIMIT 1")
        );
    }

    @Override
    public Map<String, Object> checkTrainingAllowed(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("allowed", true);
        result.put("reason", "");

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getParentId() == null) {
            return result;
        }

        // 查家长设置
        ParentSetting setting = parentSettingMapper.selectOne(
                new LambdaQueryWrapper<ParentSetting>()
                        .eq(ParentSetting::getUserId, user.getParentId())
                        .eq(ParentSetting::getChildId, userId)
        );

        if (setting != null) {
            // 检查训练锁定
            if (setting.getTrainingLock() != null && setting.getTrainingLock() == 1) {
                // 训练锁定模式下，检查是否有进行中的会话
                TrainingSession activeSession = getActiveSession(userId);
                if (activeSession == null) {
                    // 没有进行中训练，需要家长解锁
                    result.put("allowed", false);
                    result.put("reason", "训练锁定中，请家长解锁后再开始训练");
                    return result;
                }
            }

            // 检查每日训练时长限制
            if (setting.getDailyLimitMin() != null && setting.getDailyLimitMin() > 0) {
                int todayTotalSeconds = getTodayTrainingSeconds(userId);
                int limitSeconds = setting.getDailyLimitMin() * 60;
                if (todayTotalSeconds >= limitSeconds) {
                    result.put("allowed", false);
                    result.put("reason", "今日训练时长已达上限("
                            + setting.getDailyLimitMin() + "分钟)，明天再来吧！");
                    result.put("todayMinutes", todayTotalSeconds / 60);
                    result.put("limitMinutes", setting.getDailyLimitMin());
                    return result;
                }
            }
        }

        return result;
    }

    private TrainingSession getAndValidate(Long sessionId, Long userId) {
        TrainingSession session = trainingSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("训练会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此会话");
        }
        return session;
    }

    private int getTodayTrainingSeconds(Long userId) {
        LocalDate today = LocalDate.now();
        java.util.List<TrainingRecord> todayRecords = trainingRecordMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getUserId, userId)
                        .eq(TrainingRecord::getStatus, 1)
                        .ge(TrainingRecord::getEndTime, today.atStartOfDay())
                        .lt(TrainingRecord::getEndTime, today.plusDays(1).atStartOfDay())
        );
        return todayRecords.stream()
                .mapToInt(r -> r.getActualDuration() != null ? r.getActualDuration() : 0)
                .sum();
    }
}
