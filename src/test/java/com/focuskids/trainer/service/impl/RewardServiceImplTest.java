package com.focuskids.trainer.service.impl;

import com.focuskids.trainer.entity.RewardRecord;
import com.focuskids.trainer.entity.SysUser;
import com.focuskids.trainer.entity.UserStreak;
import com.focuskids.trainer.mapper.RewardRecordMapper;
import com.focuskids.trainer.mapper.SysUserMapper;
import com.focuskids.trainer.mapper.UserStreakMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardServiceImpl 单元测试")
class RewardServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private RewardRecordMapper rewardMapper;

    @Mock
    private UserStreakMapper streakMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RewardServiceImpl rewardService;

    private static final Long USER_ID = 1L;

    // ========== getStarCount ==========

    @Nested
    @DisplayName("getStarCount 测试")
    class GetStarCountTest {

        @Test
        @DisplayName("正常获取星星数量")
        void getStarCount_normal() {
            SysUser user = new SysUser();
            user.setUserId(USER_ID);
            user.setStarCount(42);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            int result = rewardService.getStarCount(USER_ID);

            assertEquals(42, result);
            verify(userMapper).selectById(USER_ID);
        }

        @Test
        @DisplayName("用户不存在时返回0")
        void getStarCount_userNotFound() {
            when(userMapper.selectById(USER_ID)).thenReturn(null);

            int result = rewardService.getStarCount(USER_ID);

            assertEquals(0, result);
        }
    }

    // ========== addStars ==========

    @Nested
    @DisplayName("addStars 测试")
    class AddStarsTest {

        @Test
        @DisplayName("正常增加星星并记录")
        void addStars_normal() {
            when(userMapper.addStars(eq(USER_ID), eq(5))).thenReturn(1);
            when(rewardMapper.insert(any(RewardRecord.class))).thenReturn(1);

            rewardService.addStars(USER_ID, 5, 1, 100L);

            verify(userMapper).addStars(USER_ID, 5);

            ArgumentCaptor<RewardRecord> captor = ArgumentCaptor.forClass(RewardRecord.class);
            verify(rewardMapper).insert(captor.capture());

            RewardRecord record = captor.getValue();
            assertEquals(USER_ID, record.getUserId());
            assertEquals(1, record.getRewardType());
            assertEquals(5, record.getRewardValue());
            assertEquals("5颗星星", record.getRewardName());
            assertEquals(1, record.getSourceType());
            assertEquals(100L, record.getSourceId());
        }
    }

    // ========== getBadges ==========

    @Nested
    @DisplayName("getBadges 测试")
    class GetBadgesTest {

        @Test
        @DisplayName("无徽章记录时返回全部未获得")
        void getBadges_noneEarned() {
            when(rewardMapper.selectList(any())).thenReturn(new ArrayList<>());

            List<Map<String, Object>> badges = rewardService.getBadges(USER_ID);

            assertEquals(12, badges.size());
            for (Map<String, Object> badge : badges) {
                assertFalse((Boolean) badge.get("earned"));
            }
        }

        @Test
        @DisplayName("已获得部分徽章")
        void getBadges_someEarned() {
            List<RewardRecord> records = new ArrayList<>();
            RewardRecord r1 = new RewardRecord();
            r1.setRewardType(2);
            r1.setRewardName("初出茅庐");
            records.add(r1);

            RewardRecord r2 = new RewardRecord();
            r2.setRewardType(2);
            r2.setRewardName("连续7天");
            records.add(r2);

            when(rewardMapper.selectList(any())).thenReturn(records);

            List<Map<String, Object>> badges = rewardService.getBadges(USER_ID);

            assertEquals(12, badges.size());

            // "初出茅庐" 是第一个，已获得
            assertTrue((Boolean) badges.get(0).get("earned"));
            // "坚持不懈" 是第二个，未获得
            assertFalse((Boolean) badges.get(1).get("earned"));
            // "连续7天" 是第8个（index=7），已获得
            assertTrue((Boolean) badges.get(7).get("earned"));
        }

        @Test
        @DisplayName("徽章列表包含正确ID和名称")
        void getBadges_correctStructure() {
            when(rewardMapper.selectList(any())).thenReturn(new ArrayList<>());

            List<Map<String, Object>> badges = rewardService.getBadges(USER_ID);

            assertEquals(1, badges.get(0).get("id"));
            assertEquals("初出茅庐", badges.get(0).get("name"));
            assertEquals(12, badges.get(11).get("id"));
            assertEquals("百星少年", badges.get(11).get("name"));
        }
    }

    // ========== getStreak ==========

    @Nested
    @DisplayName("getStreak 测试")
    class GetStreakTest {

        @Test
        @DisplayName("正常获取连续打卡记录")
        void getStreak_normal() {
            UserStreak streak = new UserStreak();
            streak.setUserId(USER_ID);
            streak.setCurrentStreak(5);
            streak.setMaxStreak(10);
            streak.setLastTrainDate(LocalDate.now().minusDays(1));

            when(streakMapper.selectOne(any())).thenReturn(streak);

            UserStreak result = rewardService.getStreak(USER_ID);

            assertEquals(5, result.getCurrentStreak());
            assertEquals(10, result.getMaxStreak());
        }

        @Test
        @DisplayName("无打卡记录时返回默认值")
        void getStreak_noRecord() {
            when(streakMapper.selectOne(any())).thenReturn(null);

            UserStreak result = rewardService.getStreak(USER_ID);

            assertNotNull(result);
            assertEquals(USER_ID, result.getUserId());
            assertEquals(0, result.getCurrentStreak());
            assertEquals(0, result.getMaxStreak());
        }
    }

    // ========== updateStreak ==========

    @Nested
    @DisplayName("updateStreak 测试")
    class UpdateStreakTest {

        @BeforeEach
        void setUp() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);
        }

        @Test
        @DisplayName("首次打卡创建记录")
        void updateStreak_firstTime() {
            when(streakMapper.selectOne(any())).thenReturn(null);
            when(rewardMapper.insert(any(RewardRecord.class))).thenReturn(1);

            UserStreak result = rewardService.updateStreak(USER_ID);

            ArgumentCaptor<UserStreak> captor = ArgumentCaptor.forClass(UserStreak.class);
            verify(streakMapper).insert(captor.capture());
            assertEquals(1, captor.getValue().getCurrentStreak());
            assertEquals(1, captor.getValue().getMaxStreak());
            assertEquals(LocalDate.now(), captor.getValue().getLastTrainDate());
        }

        @Test
        @DisplayName("连续打卡天数递增")
        void updateStreak_consecutiveDay() {
            UserStreak streak = new UserStreak();
            streak.setUserId(USER_ID);
            streak.setCurrentStreak(3);
            streak.setMaxStreak(5);
            streak.setLastTrainDate(LocalDate.now().minusDays(1));

            when(streakMapper.selectOne(any())).thenReturn(streak);

            UserStreak result = rewardService.updateStreak(USER_ID);

            verify(streakMapper).update(eq(null), any());
            verify(redisTemplate).delete("streak:lock:" + USER_ID);
        }

        @Test
        @DisplayName("今天已打卡不重复处理")
        void updateStreak_alreadyToday() {
            UserStreak streak = new UserStreak();
            streak.setUserId(USER_ID);
            streak.setCurrentStreak(3);
            streak.setMaxStreak(5);
            streak.setLastTrainDate(LocalDate.now());

            when(streakMapper.selectOne(any())).thenReturn(streak);

            rewardService.updateStreak(USER_ID);

            // 不应调用 update
            verify(streakMapper, never()).update(eq(null), any());
        }

        @Test
        @DisplayName("中断后重新开始")
        void updateStreak_broken() {
            UserStreak streak = new UserStreak();
            streak.setUserId(USER_ID);
            streak.setCurrentStreak(5);
            streak.setMaxStreak(5);
            streak.setLastTrainDate(LocalDate.now().minusDays(3));

            when(streakMapper.selectOne(any())).thenReturn(streak);

            rewardService.updateStreak(USER_ID);

            verify(streakMapper).update(eq(null), any());
        }

        @Test
        @DisplayName("分布式锁未获取时返回当前记录")
        void updateStreak_lockFailed() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            UserStreak streak = new UserStreak();
            streak.setUserId(USER_ID);
            streak.setCurrentStreak(3);
            when(streakMapper.selectOne(any())).thenReturn(streak);

            UserStreak result = rewardService.updateStreak(USER_ID);

            assertEquals(3, result.getCurrentStreak());
            // 不应执行任何数据库写入操作
            verify(streakMapper, never()).insert(any());
            verify(streakMapper, never()).update(eq(null), any());
        }

        @Test
        @DisplayName("连续7天打卡时发放徽章")
        void updateStreak_earnBadge7Days() {
            UserStreak streak = new UserStreak();
            streak.setUserId(USER_ID);
            streak.setCurrentStreak(6);
            streak.setMaxStreak(6);
            streak.setLastTrainDate(LocalDate.now().minusDays(1));

            // 模拟更新后的 streak
            UserStreak updatedStreak = new UserStreak();
            updatedStreak.setUserId(USER_ID);
            updatedStreak.setCurrentStreak(7);
            updatedStreak.setMaxStreak(7);
            updatedStreak.setLastTrainDate(LocalDate.now());

            when(streakMapper.selectOne(any())).thenReturn(streak).thenReturn(updatedStreak);
            when(rewardMapper.insert(any(RewardRecord.class))).thenReturn(1);

            rewardService.updateStreak(USER_ID);

            ArgumentCaptor<RewardRecord> captor = ArgumentCaptor.forClass(RewardRecord.class);
            verify(rewardMapper).insert(captor.capture());
            assertEquals(2, captor.getValue().getRewardType());
            assertEquals("连续7天", captor.getValue().getRewardName());
        }

        @Test
        @DisplayName("连续30天打卡时发放徽章")
        void updateStreak_earnBadge30Days() {
            UserStreak streak = new UserStreak();
            streak.setUserId(USER_ID);
            streak.setCurrentStreak(29);
            streak.setMaxStreak(29);
            streak.setLastTrainDate(LocalDate.now().minusDays(1));

            UserStreak updatedStreak = new UserStreak();
            updatedStreak.setUserId(USER_ID);
            updatedStreak.setCurrentStreak(30);
            updatedStreak.setMaxStreak(30);
            updatedStreak.setLastTrainDate(LocalDate.now());

            when(streakMapper.selectOne(any())).thenReturn(streak).thenReturn(updatedStreak);
            when(rewardMapper.insert(any(RewardRecord.class))).thenReturn(1);

            rewardService.updateStreak(USER_ID);

            ArgumentCaptor<RewardRecord> captor = ArgumentCaptor.forClass(RewardRecord.class);
            verify(rewardMapper).insert(captor.capture());
            assertEquals("连续30天", captor.getValue().getRewardName());
        }
    }
}
