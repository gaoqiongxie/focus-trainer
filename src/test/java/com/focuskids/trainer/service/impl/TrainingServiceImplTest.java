package com.focuskids.trainer.service.impl;

import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.entity.TrainingConfig;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.mapper.TrainingConfigMapper;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.service.RewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingServiceImpl 单元测试")
class TrainingServiceImplTest {

    @Mock
    private TrainingConfigMapper configMapper;

    @Mock
    private TrainingRecordMapper recordMapper;

    @Mock
    private RewardService rewardService;

    @InjectMocks
    private TrainingServiceImpl trainingService;

    private static final Long USER_ID = 1L;

    // ========== startTraining ==========

    @Nested
    @DisplayName("startTraining 测试")
    class StartTrainingTest {

        @Test
        @DisplayName("正常开始训练")
        void startTraining_normal() {
            when(recordMapper.selectCount(any())).thenReturn(0L);

            TrainingConfig config = new TrainingConfig();
            config.setTrainingType(1);
            config.setLevel(1);
            config.setDuration(300);
            config.setIsActive(1);
            when(configMapper.selectOne(any())).thenReturn(config);
            when(recordMapper.insert(any(TrainingRecord.class))).thenReturn(1);

            TrainingRecord result = trainingService.startTraining(USER_ID, 1, 1, null);

            assertNotNull(result);
            assertEquals(USER_ID, result.getUserId());
            assertEquals(1, result.getTrainingType());
            assertEquals(1, result.getLevel());
            assertEquals(300, result.getDuration()); // 使用配置默认时长
            assertEquals(0, result.getStatus());
            assertEquals(0, result.getActualDuration());
            assertEquals(0, result.getInterruptCount());
        }

        @Test
        @DisplayName("指定duration时使用指定时长")
        void startTraining_customDuration() {
            when(recordMapper.selectCount(any())).thenReturn(0L);

            TrainingConfig config = new TrainingConfig();
            config.setTrainingType(2);
            config.setLevel(3);
            config.setDuration(600);
            config.setIsActive(1);
            when(configMapper.selectOne(any())).thenReturn(config);
            when(recordMapper.insert(any(TrainingRecord.class))).thenReturn(1);

            TrainingRecord result = trainingService.startTraining(USER_ID, 2, 3, 900);

            assertEquals(900, result.getDuration());
        }

        @Test
        @DisplayName("已有进行中训练时抛出异常")
        void startTraining_alreadyInProgress() {
            when(recordMapper.selectCount(any())).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> trainingService.startTraining(USER_ID, 1, 1, null));

            assertEquals(ErrorCode.TRAINING_IN_PROGRESS.getCode(), ex.getCode());
            verify(configMapper, never()).selectOne(any());
            verify(recordMapper, never()).insert(any());
        }

        @Test
        @DisplayName("训练配置不存在时抛出异常")
        void startTraining_configNotFound() {
            when(recordMapper.selectCount(any())).thenReturn(0L);
            when(configMapper.selectOne(any())).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> trainingService.startTraining(USER_ID, 999, 1, null));

            assertEquals(ErrorCode.TRAINING_CONFIG_ERROR.getCode(), ex.getCode());
        }
    }

    // ========== completeTraining ==========

    @Nested
    @DisplayName("completeTraining 测试")
    class CompleteTrainingTest {

        private final Long RECORD_ID = 10L;

        private TrainingRecord buildInProgressRecord() {
            TrainingRecord record = new TrainingRecord();
            record.setRecordId(RECORD_ID);
            record.setUserId(USER_ID);
            record.setTrainingType(1);
            record.setLevel(1);
            record.setStatus(0);
            record.setStartTime(LocalDateTime.now().minusMinutes(5));
            return record;
        }

        @Test
        @DisplayName("正常完成训练 - 高正确率")
        void completeTraining_highAccuracy() {
            TrainingRecord record = buildInProgressRecord();
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);
            when(recordMapper.updateById(any(TrainingRecord.class))).thenReturn(1);
            doNothing().when(rewardService).addStars(anyLong(), anyInt(), anyInt(), anyLong());

            TrainingRecord result = trainingService.completeTraining(USER_ID, RECORD_ID, 300, 0, 85.0, 90);

            ArgumentCaptor<TrainingRecord> captor = ArgumentCaptor.forClass(TrainingRecord.class);
            verify(recordMapper).updateById(captor.capture());

            TrainingRecord updated = captor.getValue();
            assertEquals(1, updated.getStatus());
            assertNotNull(updated.getEndTime());
            assertEquals(BigDecimal.valueOf(85.00).setScale(2), updated.getAccuracy());
            assertEquals(90, updated.getScore());

            // 星星计算: 300/60*2 = 10, 正确率>=80% +5 = 15
            assertEquals(15, updated.getStarReward());

            // 验证发放星星
            verify(rewardService).addStars(USER_ID, 15, 1, RECORD_ID);
        }

        @Test
        @DisplayName("正常完成训练 - 中正确率(60-79%)")
        void completeTraining_mediumAccuracy() {
            TrainingRecord record = buildInProgressRecord();
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);
            when(recordMapper.updateById(any())).thenReturn(1);
            doNothing().when(rewardService).addStars(anyLong(), anyInt(), anyInt(), anyLong());

            trainingService.completeTraining(USER_ID, RECORD_ID, 300, 2, 65.0, 70);

            ArgumentCaptor<TrainingRecord> captor = ArgumentCaptor.forClass(TrainingRecord.class);
            verify(recordMapper).updateById(captor.capture());

            // 星星: 300/60*2 = 10, 正确率60-79% +2 = 12
            assertEquals(12, captor.getValue().getStarReward());
        }

        @Test
        @DisplayName("正常完成训练 - 低正确率(<60%)")
        void completeTraining_lowAccuracy() {
            TrainingRecord record = buildInProgressRecord();
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);
            when(recordMapper.updateById(any())).thenReturn(1);
            doNothing().when(rewardService).addStars(anyLong(), anyInt(), anyInt(), anyLong());

            trainingService.completeTraining(USER_ID, RECORD_ID, 300, 0, 40.0, 50);

            ArgumentCaptor<TrainingRecord> captor = ArgumentCaptor.forClass(TrainingRecord.class);
            verify(recordMapper).updateById(captor.capture());

            // 星星: 300/60*2 = 10, 正确率<60% 无额外奖励
            assertEquals(10, captor.getValue().getStarReward());
        }

        @Test
        @DisplayName("accuracy为null时星星只计算基础分")
        void completeTraining_nullAccuracy() {
            TrainingRecord record = buildInProgressRecord();
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);
            when(recordMapper.updateById(any())).thenReturn(1);
            doNothing().when(rewardService).addStars(anyLong(), anyInt(), anyInt(), anyLong());

            trainingService.completeTraining(USER_ID, RECORD_ID, 300, 0, null, 50);

            ArgumentCaptor<TrainingRecord> captor = ArgumentCaptor.forClass(TrainingRecord.class);
            verify(recordMapper).updateById(captor.capture());

            // 星星: 300/60*2 = 10
            assertEquals(10, captor.getValue().getStarReward());
            assertNull(captor.getValue().getAccuracy());
        }

        @Test
        @DisplayName("训练记录不存在时抛出异常")
        void completeTraining_notFound() {
            when(recordMapper.selectById(RECORD_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> trainingService.completeTraining(USER_ID, RECORD_ID, 300, 0, 80.0, 80));

            assertEquals(ErrorCode.TRAINING_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非本人训练记录时抛出异常")
        void completeTraining_forbidden() {
            TrainingRecord record = buildInProgressRecord();
            record.setUserId(999L); // 其他用户的记录
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> trainingService.completeTraining(USER_ID, RECORD_ID, 300, 0, 80.0, 80));

            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("训练已完成时抛出异常")
        void completeTraining_alreadyCompleted() {
            TrainingRecord record = buildInProgressRecord();
            record.setStatus(1); // 已完成
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> trainingService.completeTraining(USER_ID, RECORD_ID, 300, 0, 80.0, 80));

            assertEquals(ErrorCode.TRAINING_ALREADY_COMPLETED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("accuracy边界值验证 - 负值被修正为0")
        void completeTraining_negativeAccuracy() {
            TrainingRecord record = buildInProgressRecord();
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);
            when(recordMapper.updateById(any())).thenReturn(1);
            doNothing().when(rewardService).addStars(anyLong(), anyInt(), anyInt(), anyLong());

            trainingService.completeTraining(USER_ID, RECORD_ID, 300, 0, -10.0, 50);

            ArgumentCaptor<TrainingRecord> captor = ArgumentCaptor.forClass(TrainingRecord.class);
            verify(recordMapper).updateById(captor.capture());
            assertEquals(BigDecimal.valueOf(0.00).setScale(2), captor.getValue().getAccuracy());
        }

        @Test
        @DisplayName("accuracy边界值验证 - 超过100被修正为100")
        void completeTraining_over100Accuracy() {
            TrainingRecord record = buildInProgressRecord();
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);
            when(recordMapper.updateById(any())).thenReturn(1);
            doNothing().when(rewardService).addStars(anyLong(), anyInt(), anyInt(), anyLong());

            trainingService.completeTraining(USER_ID, RECORD_ID, 300, 0, 150.0, 50);

            ArgumentCaptor<TrainingRecord> captor = ArgumentCaptor.forClass(TrainingRecord.class);
            verify(recordMapper).updateById(captor.capture());
            assertEquals(BigDecimal.valueOf(100.00).setScale(2), captor.getValue().getAccuracy());
        }
    }

    // ========== interruptTraining ==========

    @Nested
    @DisplayName("interruptTraining 测试")
    class InterruptTrainingTest {

        private final Long RECORD_ID = 10L;

        private TrainingRecord buildInProgressRecord() {
            TrainingRecord record = new TrainingRecord();
            record.setRecordId(RECORD_ID);
            record.setUserId(USER_ID);
            record.setTrainingType(1);
            record.setStatus(0);
            record.setStartTime(LocalDateTime.now().minusMinutes(3));
            record.setActualDuration(0);
            return record;
        }

        @Test
        @DisplayName("正常中断训练")
        void interruptTraining_normal() {
            TrainingRecord record = buildInProgressRecord();
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);
            when(recordMapper.updateById(any(TrainingRecord.class))).thenReturn(1);

            trainingService.interruptTraining(USER_ID, RECORD_ID);

            ArgumentCaptor<TrainingRecord> captor = ArgumentCaptor.forClass(TrainingRecord.class);
            verify(recordMapper).updateById(captor.capture());

            TrainingRecord updated = captor.getValue();
            assertEquals(2, updated.getStatus());
            assertNotNull(updated.getEndTime());
            assertNotNull(updated.getActualDuration());
            assertTrue(updated.getActualDuration() > 0);
        }

        @Test
        @DisplayName("有actualDuration时使用原值")
        void interruptTraining_withActualDuration() {
            TrainingRecord record = buildInProgressRecord();
            record.setActualDuration(120);
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);
            when(recordMapper.updateById(any(TrainingRecord.class))).thenReturn(1);

            trainingService.interruptTraining(USER_ID, RECORD_ID);

            ArgumentCaptor<TrainingRecord> captor = ArgumentCaptor.forClass(TrainingRecord.class);
            verify(recordMapper).updateById(captor.capture());
            assertEquals(120, captor.getValue().getActualDuration());
        }

        @Test
        @DisplayName("训练记录不存在时抛出异常")
        void interruptTraining_notFound() {
            when(recordMapper.selectById(RECORD_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> trainingService.interruptTraining(USER_ID, RECORD_ID));

            assertEquals(ErrorCode.TRAINING_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非本人训练记录时抛出异常")
        void interruptTraining_forbidden() {
            TrainingRecord record = buildInProgressRecord();
            record.setUserId(999L);
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> trainingService.interruptTraining(USER_ID, RECORD_ID));

            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("训练已完成时静默返回")
        void interruptTraining_alreadyCompleted() {
            TrainingRecord record = buildInProgressRecord();
            record.setStatus(1); // 已完成
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);

            trainingService.interruptTraining(USER_ID, RECORD_ID);

            verify(recordMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("训练已中断时静默返回")
        void interruptTraining_alreadyInterrupted() {
            TrainingRecord record = buildInProgressRecord();
            record.setStatus(2); // 已中断
            when(recordMapper.selectById(RECORD_ID)).thenReturn(record);

            trainingService.interruptTraining(USER_ID, RECORD_ID);

            verify(recordMapper, never()).updateById(any());
        }
    }

    // ========== getConfigList ==========

    @Nested
    @DisplayName("getConfigList 测试")
    class GetConfigListTest {

        @Test
        @DisplayName("按类型查询配置")
        void getConfigList_byType() {
            TrainingConfig c1 = new TrainingConfig();
            c1.setTrainingType(1);
            c1.setLevel(1);
            TrainingConfig c2 = new TrainingConfig();
            c2.setTrainingType(1);
            c2.setLevel(2);

            when(configMapper.selectActiveByType(1)).thenReturn(Arrays.asList(c1, c2));

            List<TrainingConfig> result = trainingService.getConfigList(1);

            assertEquals(2, result.size());
            verify(configMapper).selectActiveByType(1);
        }

        @Test
        @DisplayName("不传类型查询全部配置")
        void getConfigList_allTypes() {
            TrainingConfig c1 = new TrainingConfig();
            c1.setTrainingType(1);
            TrainingConfig c2 = new TrainingConfig();
            c2.setTrainingType(2);

            when(configMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2));

            List<TrainingConfig> result = trainingService.getConfigList(null);

            assertEquals(2, result.size());
            verify(configMapper).selectList(any());
        }
    }

    // ========== getStatistics ==========

    @Nested
    @DisplayName("getStatistics 测试")
    class GetStatisticsTest {

        @Test
        @DisplayName("有训练数据时返回统计")
        void getStatistics_withData() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCount", 10);
            stats.put("completedCount", 8);
            stats.put("totalDuration", 6000);
            stats.put("avgAccuracy", 75.5);
            stats.put("totalStars", 40);

            when(recordMapper.selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(stats);

            Map<String, Object> result = trainingService.getStatistics(USER_ID, "week");

            assertEquals(80.0, result.get("completionRate"));
            assertEquals("week", result.get("period"));
        }

        @Test
        @DisplayName("无训练数据时返回默认值")
        void getStatistics_noData() {
            when(recordMapper.selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(null);

            Map<String, Object> result = trainingService.getStatistics(USER_ID, "week");

            assertEquals(0, result.get("totalCount"));
            assertEquals(0, result.get("completionRate"));
        }

        @Test
        @DisplayName("按月统计时间范围正确")
        void getStatistics_monthPeriod() {
            when(recordMapper.selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(null);

            trainingService.getStatistics(USER_ID, "month");

            verify(recordMapper).selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }
}
