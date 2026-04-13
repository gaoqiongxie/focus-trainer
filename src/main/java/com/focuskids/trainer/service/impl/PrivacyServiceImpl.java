package com.focuskids.trainer.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.entity.*;
import com.focuskids.trainer.mapper.*;
import com.focuskids.trainer.service.PrivacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyServiceImpl implements PrivacyService {

    private final SysUserMapper sysUserMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final UserAbilityMapper userAbilityMapper;
    private final RewardRecordMapper rewardRecordMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final UserStreakMapper userStreakMapper;
    private final DailyTaskMapper dailyTaskMapper;
    private final NotificationMapper notificationMapper;
    private final DataExportRecordMapper dataExportRecordMapper;
    private final UserTitleRecordMapper userTitleRecordMapper;
    private final TrainingSessionMapper trainingSessionMapper;
    private final ParentSettingMapper parentSettingMapper;

    @Value("${app.export.dir:./exports}")
    private String exportDir;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> exportUserData(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 收集所有用户数据
        Map<String, Object> userData = new LinkedHashMap<>();

        // 基本信息（脱敏）
        Map<String, Object> basicInfo = new LinkedHashMap<>();
        basicInfo.put("userId", user.getUserId());
        basicInfo.put("nickname", user.getNickname());
        basicInfo.put("userType", user.getUserType());
        basicInfo.put("age", user.getAge());
        basicInfo.put("gender", user.getGender());
        basicInfo.put("grade", user.getGrade());
        basicInfo.put("starCount", user.getStarCount());
        basicInfo.put("createTime", user.getCreateTime());
        userData.put("basicInfo", basicInfo);

        // 训练记录
        List<TrainingRecord> records = trainingRecordMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>().eq(TrainingRecord::getUserId, userId));
        userData.put("trainingRecords", records);

        // 能力评估
        List<UserAbility> abilities = userAbilityMapper.selectList(
                new LambdaQueryWrapper<UserAbility>().eq(UserAbility::getUserId, userId));
        userData.put("abilityEvaluations", abilities);

        // 奖励记录
        List<RewardRecord> rewards = rewardRecordMapper.selectList(
                new LambdaQueryWrapper<RewardRecord>().eq(RewardRecord::getUserId, userId));
        userData.put("rewardRecords", rewards);

        // 徽章记录
        List<UserBadge> badges = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>().eq(UserBadge::getUserId, userId));
        userData.put("badgeRecords", badges);

        // 连续打卡
        UserStreak streak = userStreakMapper.selectOne(
                new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, userId));
        userData.put("streakInfo", streak);

        // 每日任务
        List<DailyTask> tasks = dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>().eq(DailyTask::getUserId, userId));
        userData.put("dailyTasks", tasks);

        // 称号记录
        List<UserTitleRecord> titles = userTitleRecordMapper.selectList(
                new LambdaQueryWrapper<UserTitleRecord>().eq(UserTitleRecord::getUserId, userId));
        userData.put("titleRecords", titles);

        // 写入文件
        try {
            File dir = new File(exportDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "user_data_" + userId + "_" + timestamp + ".json";
            File exportFile = new File(dir, fileName);

            try (FileWriter writer = new FileWriter(exportFile)) {
                writer.write(JSONUtil.toJsonPrettyStr(userData));
            }

            // 记录导出
            DataExportRecord exportRecord = new DataExportRecord();
            exportRecord.setUserId(userId);
            exportRecord.setFilePath(exportFile.getAbsolutePath());
            exportRecord.setFileSize(exportFile.length());
            exportRecord.setStatus(1);
            exportRecord.setExpireTime(LocalDateTime.now().plusDays(7));
            dataExportRecordMapper.insert(exportRecord);

            Map<String, Object> result = new HashMap<>();
            result.put("exportId", exportRecord.getExportId());
            result.put("filePath", exportFile.getAbsolutePath());
            result.put("fileSize", exportFile.length());
            result.put("expireTime", exportRecord.getExpireTime());
            return result;

        } catch (IOException e) {
            log.error("导出用户数据失败, userId={}", userId, e);
            throw new BusinessException("数据导出失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserData(Long userId, Long parentId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 校验操作权限：必须是该用户的家长
        if (user.getParentId() == null || !user.getParentId().equals(parentId)) {
            throw new BusinessException(403, "无权操作，仅关联家长可删除孩子数据");
        }

        log.warn("开始删除用户数据, userId={}, operatorId={}", userId, parentId);

        // 按顺序删除关联数据
        trainingSessionMapper.delete(new LambdaQueryWrapper<TrainingSession>()
                .eq(TrainingSession::getUserId, userId));
        trainingRecordMapper.delete(new LambdaQueryWrapper<TrainingRecord>()
                .eq(TrainingRecord::getUserId, userId));
        userAbilityMapper.delete(new LambdaQueryWrapper<UserAbility>()
                .eq(UserAbility::getUserId, userId));
        rewardRecordMapper.delete(new LambdaQueryWrapper<RewardRecord>()
                .eq(RewardRecord::getUserId, userId));
        userBadgeMapper.delete(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId));
        userStreakMapper.delete(new LambdaQueryWrapper<UserStreak>()
                .eq(UserStreak::getUserId, userId));
        dailyTaskMapper.delete(new LambdaQueryWrapper<DailyTask>()
                .eq(DailyTask::getUserId, userId));
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId));
        userTitleRecordMapper.delete(new LambdaQueryWrapper<UserTitleRecord>()
                .eq(UserTitleRecord::getUserId, userId));
        dataExportRecordMapper.delete(new LambdaQueryWrapper<DataExportRecord>()
                .eq(DataExportRecord::getUserId, userId));
        parentSettingMapper.delete(new LambdaQueryWrapper<ParentSetting>()
                .eq(ParentSetting::getChildId, userId));

        // 最后逻辑删除用户
        sysUserMapper.deleteById(userId);

        log.info("用户数据删除完成, userId={}", userId);
    }
}
