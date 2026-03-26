package com.focuskids.trainer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focuskids.trainer.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询家长下的儿童列表
     */
    List<SysUser> selectChildrenByParentId(@Param("parentId") Long parentId);

    /**
     * 原子增加星星数量，避免并发竞态
     */
    @Update("UPDATE sys_user SET star_count = star_count + #{count} WHERE user_id = #{userId} AND deleted = 0")
    int addStars(@Param("userId") Long userId, @Param("count") int count);
}
