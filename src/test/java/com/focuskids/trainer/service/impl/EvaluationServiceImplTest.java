package com.focuskids.trainer.service.impl;

import com.focuskids.trainer.common.api.BusinessException;
import com.focuskids.trainer.common.api.ErrorCode;
import com.focuskids.trainer.entity.TrainingRecord;
import com.focuskids.trainer.entity.UserAbility;
import com.focuskids.trainer.mapper.TrainingRecordMapper;
import com.focuskids.trainer.mapper.UserAbilityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationServiceImpl 单元测试")
class EvaluationServiceImplTest {

    @Mock
    private UserAbilityMapper abilityMapper;

    @Mock
    private TrainingRecordMapper recordMapper;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private static final Long USER_ID = 1L;

    // ========== initEvaluation ==========

    @Nested
    @DisplayName("initEvaluation 测试")
    class InitEvaluationTest {

        @Test
        @DisplayName("首次初始化评估记录")
        void initEvaluation_firstTime() {
            when(abilityMapper.selectCount(any())).thenReturn(0L);
            when(abilityMapper.insert(any(UserAbility.class))).thenReturn(1);

            evaluationService.initEvaluation(USER_ID);

            ArgumentCaptor<UserAbility> captor = ArgumentCaptor.forClass(UserAbility.class);
            verify(abilityMapper).insert(captor.capture());

            UserAbility ability = captor.getValue();
            assertEquals(USER_ID, ability.getUserId());
            assertEquals(LocalDate.now(), ability.getEvaluateDate());
            assertEquals(BigDecimal.ZERO, ability.getAttentionDuration());
            assertEquals(BigDecimal.ZERO, ability.getVisualAttention());
            assertEquals(BigDecimal.ZERO, ability.getAuditoryAttention());
            assertEquals(BigDecimal.ZERO, ability.getWorkingMemory());
            assertEquals(BigDecimal.ZERO, ability.getInhibitoryControl());
            assertEquals(BigDecimal.ZERO, ability.getTotalScore());
            assertEquals("E", ability.getAbilityLevel());
        }

        @Test
        @DisplayName("今天已有评估记录时不重复创建")
        void initEvaluation_alreadyExists() {
            when(abilityMapper.selectCount(any())).thenReturn(1L);

            evaluationService.initEvaluation(USER_ID);

            verify(abilityMapper, never()).insert(any());
        }
    }

    // ========== generateEvaluation ==========

    @Nested
    @DisplayName("generateEvaluation 测试")
    class GenerateEvaluationTest {

        private Map<String, Object> buildStats(int totalCount, int completedCount, int totalDuration, double avgAccuracy) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCount", totalCount);
            stats.put("completedCount", completedCount);
            stats.put("totalDuration", totalDuration);
            stats.put("avgAccuracy", avgAccuracy);
            return stats;
        }

        @Test
        @DisplayName("正常生成评估 - A级")
        void generateEvaluation_gradeA() {
            when(abilityMapper.selectCount(any())).thenReturn(0L);

            // 构建7天训练数据统计
            Map<String, Object> stats = buildStats(10, 10, 20000, 95.0);
            when(recordMapper.selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(stats);

            // 模拟各类型训练记录 - 全部高正确率
            when(recordMapper.selectList(any())).thenReturn(
                    buildRecords(90.0, 5),   // type 2: 视觉
                    buildRecords(90.0, 3),   // type 3: 听觉
                    buildRecords(90.0, 2)    // type 4: 记忆
            );

            when(abilityMapper.insert(any(UserAbility.class))).thenReturn(1);

            UserAbility result = evaluationService.generateEvaluation(USER_ID);

            ArgumentCaptor<UserAbility> captor = ArgumentCaptor.forClass(UserAbility.class);
            verify(abilityMapper).insert(captor.capture());

            UserAbility ability = captor.getValue();
            assertEquals("A", ability.getAbilityLevel());
            assertNotNull(ability.getTotalScore());
            assertTrue(ability.getTotalScore().doubleValue() >= 90);
        }

        @Test
        @DisplayName("正常生成评估 - E级（低分）")
        void generateEvaluation_gradeE() {
            when(abilityMapper.selectCount(any())).thenReturn(0L);

            Map<String, Object> stats = buildStats(10, 5, 600, 30.0);
            when(recordMapper.selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(stats);

            // 各类型无训练记录 → 返回空列表
            when(recordMapper.selectList(any())).thenReturn(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );

            when(abilityMapper.insert(any(UserAbility.class))).thenReturn(1);

            UserAbility result = evaluationService.generateEvaluation(USER_ID);

            ArgumentCaptor<UserAbility> captor = ArgumentCaptor.forClass(UserAbility.class);
            verify(abilityMapper).insert(captor.capture());
            assertEquals("E", captor.getValue().getAbilityLevel());
        }

        @Test
        @DisplayName("无训练数据时抛出异常")
        void generateEvaluation_noTrainingData() {
            when(abilityMapper.selectCount(any())).thenReturn(0L);
            when(recordMapper.selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> evaluationService.generateEvaluation(USER_ID));

            assertEquals(ErrorCode.EVALUATION_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("训练数据totalCount为0时抛出异常")
        void generateEvaluation_zeroTotalCount() {
            when(abilityMapper.selectCount(any())).thenReturn(0L);

            Map<String, Object> stats = buildStats(0, 0, 0, 0.0);
            when(recordMapper.selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(stats);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> evaluationService.generateEvaluation(USER_ID));

            assertEquals(ErrorCode.EVALUATION_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("今天已有评估记录时抛出异常")
        void generateEvaluation_alreadyExists() {
            when(abilityMapper.selectCount(any())).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> evaluationService.generateEvaluation(USER_ID));

            assertEquals(ErrorCode.EVALUATION_IN_PROGRESS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("各维度得分计算正确性验证")
        void generateEvaluation_scoreCalculation() {
            when(abilityMapper.selectCount(any())).thenReturn(0L);

            // 10次完成训练，每次平均300秒，总时长3000秒
            Map<String, Object> stats = buildStats(10, 10, 3000, 80.0);
            when(recordMapper.selectTrainingStatistics(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(stats);

            // 视觉: 80%
            // 听觉: 60%
            // 记忆: 0 (空列表)
            when(recordMapper.selectList(any())).thenReturn(
                    buildRecords(80.0, 5),
                    buildRecords(60.0, 3),
                    Collections.emptyList()
            );

            when(abilityMapper.insert(any(UserAbility.class))).thenReturn(1);

            evaluationService.generateEvaluation(USER_ID);

            ArgumentCaptor<UserAbility> captor = ArgumentCaptor.forClass(UserAbility.class);
            verify(abilityMapper).insert(captor.capture());
            UserAbility ability = captor.getValue();

            // 专注时长: 3000/10 = 300秒, 300/1200*100 = 25
            assertEquals(new BigDecimal("25.00"), ability.getAttentionDuration());
            // 视觉: 80
            assertEquals(new BigDecimal("80.00"), ability.getVisualAttention());
            // 听觉: 60
            assertEquals(new BigDecimal("60.00"), ability.getAuditoryAttention());
            // 记忆: 0 (空列表)
            assertEquals(new BigDecimal("0.00"), ability.getWorkingMemory());
            // 抑制控制: 完成率100%, 100-0 = 100
            assertEquals(new BigDecimal("100.00"), ability.getInhibitoryControl());
        }
    }

    // ========== getLatestEvaluation ==========

    @Nested
    @DisplayName("getLatestEvaluation 测试")
    class GetLatestEvaluationTest {

        @Test
        @DisplayName("正常获取最新评估")
        void getLatestEvaluation_normal() {
            UserAbility ability = new UserAbility();
            ability.setUserId(USER_ID);
            ability.setTotalScore(new BigDecimal("85.5"));
            ability.setAbilityLevel("B");

            when(abilityMapper.selectOne(any())).thenReturn(ability);

            UserAbility result = evaluationService.getLatestEvaluation(USER_ID);

            assertNotNull(result);
            assertEquals(new BigDecimal("85.5"), result.getTotalScore());
            assertEquals("B", result.getAbilityLevel());
        }

        @Test
        @DisplayName("无评估记录时返回null")
        void getLatestEvaluation_notFound() {
            when(abilityMapper.selectOne(any())).thenReturn(null);

            UserAbility result = evaluationService.getLatestEvaluation(USER_ID);

            assertNull(result);
        }
    }

    // ========== getEvaluationHistory ==========

    @Nested
    @DisplayName("getEvaluationHistory 测试")
    class GetEvaluationHistoryTest {

        @Test
        @DisplayName("正常获取评估历史")
        void getEvaluationHistory_normal() {
            UserAbility a1 = new UserAbility();
            a1.setTotalScore(new BigDecimal("70.0"));
            UserAbility a2 = new UserAbility();
            a2.setTotalScore(new BigDecimal("80.0"));

            when(abilityMapper.selectHistoryByUserId(eq(USER_ID), eq(10)))
                    .thenReturn(Arrays.asList(a1, a2));

            List<UserAbility> result = evaluationService.getEvaluationHistory(USER_ID, 10);

            assertEquals(2, result.size());
            verify(abilityMapper).selectHistoryByUserId(USER_ID, 10);
        }
    }

    // ========== helper ==========

    private List<TrainingRecord> buildRecords(double accuracy, int count) {
        List<TrainingRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TrainingRecord r = new TrainingRecord();
            r.setUserId(USER_ID);
            r.setStatus(1);
            r.setAccuracy(BigDecimal.valueOf(accuracy));
            r.setStartTime(LocalDateTime.now().minusDays(i));
            records.add(r);
        }
        return records;
    }
}
