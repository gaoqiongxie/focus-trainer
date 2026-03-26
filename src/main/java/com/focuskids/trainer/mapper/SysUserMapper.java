package com.focuskids.trainer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focuskids.trainer.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询家长下的儿童列表
     */
    List<SysUser> selectChildrenByParentId(@Param("parentId") Long parentId);
}
