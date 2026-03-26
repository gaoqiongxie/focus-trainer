package com.focuskids.trainer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focuskids.trainer.entity.TrainingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TrainingRecordMapper extends BaseMapper<TrainingRecord> {

    /**
     * 统计用户某时间段的训练数据
     */
    Map<String, Object> selectTrainingStatistics(@Param("userId") Long userId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询用户最近的训练记录
     */
    List<TrainingRecord> selectRecentRecords(@Param("userId") Long userId,
                                              @Param("limit") int limit);
}
