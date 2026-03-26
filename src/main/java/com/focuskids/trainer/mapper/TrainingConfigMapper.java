package com.focuskids.trainer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focuskids.trainer.entity.TrainingConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrainingConfigMapper extends BaseMapper<TrainingConfig> {

    /**
     * 根据训练类型查询可用配置列表
     */
    List<TrainingConfig> selectActiveByType(@Param("trainingType") Integer trainingType);
}
