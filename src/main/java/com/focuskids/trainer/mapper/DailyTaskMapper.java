package com.focuskids.trainer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focuskids.trainer.entity.DailyTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日任务 Mapper
 */
@Mapper
public interface DailyTaskMapper extends BaseMapper<DailyTask> {

    /**
     * 查询用户某天的任务列表
     */
    List<DailyTask> selectByUserIdAndDate(@Param("userId") Long userId,
                                           @Param("taskDate") LocalDate taskDate);
}
