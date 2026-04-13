package com.focuskids.trainer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focuskids.trainer.entity.UserBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBadgeMapper extends BaseMapper<UserBadge> {

    /**
     * 查询用户已获得的徽章ID列表
     */
    @Select("SELECT badge_id FROM user_badge WHERE user_id = #{userId}")
    List<Integer> selectBadgeIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户已获得的徽章数量
     */
    @Select("SELECT COUNT(*) FROM user_badge WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}
