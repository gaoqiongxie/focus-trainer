package com.focuskids.trainer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focuskids.trainer.entity.UserAbility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAbilityMapper extends BaseMapper<UserAbility> {

    /**
     * 查询用户评估历史
     */
    List<UserAbility> selectHistoryByUserId(@Param("userId") Long userId,
                                             @Param("limit") int limit);
}
