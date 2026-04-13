package com.focuskids.trainer.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.entity.Notification;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.entity.UserAbility;
import com.focuskids.trainer.entity.UserStreak;
import com.focuskids.trainer.mapper.NotificationMapper;
import com.focuskids.trainer.mapper.SysUserMapper;
import com.focuskids.trainer.mapper.UserAbilityMapper;
import com.focuskids.trainer.mapper.UserStreakMapper;
import com.focuskids.trainer.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 定时任务：每天8:00发送训练提醒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTask {

    private final SysUserMapper sysUserMapper;
    private final UserStreakMapper userStreakMapper;
    private final UserAbilityMapper userAbilityMapper;
    private final NotificationService notificationService;

    /**
     * 每天8:00发送训练提醒给连续打卡<3天的用户
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendTrainingReminder() {
        log.info("[定时任务] 开始发送每日训练提醒...");

        // 查询所有儿童用户
        List<SysUser> children = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserType, 1)
                        .eq(SysUser::getStatus, 1)
        );

        int sentCount = 0;
        for (SysUser child : children) {
            // 检查连续打卡天数
            UserStreak streak = userStreakMapper.selectOne(
                    new LambdaQueryWrapper<UserStreak>()
                            .eq(UserStreak::getUserId, child.getUserId())
            );

            int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
            if (currentStreak < 3) {
                // 查找家长
                if (child.getParentId() != null) {
                    String msg;
                    if (currentStreak == 0) {
                        msg = child.getNickname() + "还没有开始训练哦，快来一起练习吧！坚持每天训练可以提升注意力~";
                    } else {
                        msg = child.getNickname() + "已经连续训练" + currentStreak + "天了，再坚持一下就能获得新徽章！";
                    }
                    notificationService.send(child.getParentId(), 1, "训练提醒", msg);
                    sentCount++;
                }
            }
        }

        log.info("[定时任务] 训练提醒发送完成，共发送{}条", sentCount);
    }

    /**
     * 每周一9:00发送专家建议（基于能力评估薄弱项）
     */
    @Scheduled(cron = "0 0 9 ? * MON")
    public void sendExpertAdvice() {
        log.info("[定时任务] 开始发送每周专家建议...");

        List<SysUser> children = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserType, 1)
                        .eq(SysUser::getStatus, 1)
        );

        int sentCount = 0;
        for (SysUser child : children) {
            if (child.getParentId() == null) {
                continue;
            }

            UserAbility ability = userAbilityMapper.selectOne(
                    new LambdaQueryWrapper<UserAbility>()
                            .eq(UserAbility::getUserId, child.getUserId())
                            .orderByDesc(UserAbility::getEvaluateDate)
                            .last("LIMIT 1")
            );

            if (ability == null) {
                continue;
            }

            // 找到最薄弱项
            String weakest = findWeakestDimension(ability);
            if (weakest != null) {
                String advice = generateAdvice(child.getNickname(), weakest, ability);
                notificationService.send(child.getParentId(), 3, "专家建议", advice);
                sentCount++;
            }
        }

        log.info("[定时任务] 专家建议发送完成，共发送{}条", sentCount);
    }

    private String findWeakestDimension(UserAbility ability) {
        // 5维：attentionDuration, visualAttention, auditoryAttention, workingMemory, inhibitoryControl
        String[] names = {"专注时长", "视觉注意力", "听觉注意力", "工作记忆", "抑制控制"};
        BigDecimal[] values = {
                ability.getAttentionDuration(),
                ability.getVisualAttention(),
                ability.getAuditoryAttention(),
                ability.getWorkingMemory(),
                ability.getInhibitoryControl()
        };

        BigDecimal min = null;
        int minIdx = -1;
        for (int i = 0; i < values.length; i++) {
            BigDecimal v = values[i] != null ? values[i] : BigDecimal.ZERO;
            if (min == null || v.compareTo(min) < 0) {
                min = v;
                minIdx = i;
            }
        }

        // 如果最低分也>=70，则不需要建议
        if (min != null && min.compareTo(new BigDecimal("70")) >= 0) {
            return null;
        }
        return minIdx >= 0 ? names[minIdx] : null;
    }

    private String generateAdvice(String nickname, String dimension, UserAbility ability) {
        StringBuilder sb = new StringBuilder();
        sb.append(nickname).append("的").append(dimension).append("能力相对薄弱，建议：\n");

        switch (dimension) {
            case "专注时长":
                sb.append("1. 从短时间训练开始（5分钟），逐步增加时长\n");
                sb.append("2. 在安静环境中训练，减少干扰\n");
                sb.append("3. 尝试专注时长训练游戏");
                break;
            case "视觉注意力":
                sb.append("1. 多练习舒尔特方格和数字闪现\n");
                sb.append("2. 可以先从低难度3x3方格开始\n");
                sb.append("3. 每天练习1-2组视觉追踪游戏");
                break;
            case "听觉注意力":
                sb.append("1. 多练习声音序列游戏\n");
                sb.append("2. 从3个声音的短序列开始\n");
                sb.append("3. 训练时保持安静，认真听每个声音");
                break;
            case "工作记忆":
                sb.append("1. 卡片配对游戏是很好的记忆训练\n");
                sb.append("2. 从6对卡片开始，逐步增加难度\n");
                sb.append("3. 记忆卡片位置时尝试分段记忆");
                break;
            case "抑制控制":
                sb.append("1. 保持稳定的训练节奏\n");
                sb.append("2. 尽量减少训练中的中断次数\n");
                sb.append("3. 专注时长训练可以提升自控能力");
                break;
            default:
                sb.append("保持每日训练，能力会逐步提升！");
        }
        return sb.toString();
    }
}
