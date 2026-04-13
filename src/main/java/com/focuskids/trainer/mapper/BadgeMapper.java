package com.focuskids.trainer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focuskids.trainer.entity.Badge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BadgeMapper extends BaseMapper<Badge> {

    /**
     * 查询所有启用的徽章，按展示顺序排序
     */
    @Select("SELECT * FROM badge WHERE is_active = 1 ORDER BY display_order ASC")
    List<Badge> selectAllActive();

    /**
     * 查询指定类别的徽章
     */
    @Select("SELECT * FROM badge WHERE is_active = 1 AND category = #{category} ORDER BY display_order ASC")
    List<Badge> selectByCategory(@Param("category") String category);
}
