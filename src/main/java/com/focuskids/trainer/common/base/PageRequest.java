package com.focuskids.trainer.common.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.Data;

import java.util.List;

/**
 * 分页请求
 */
@Data
public class PageRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    public <T> IPage<T> toPage() {
        return new Page<>(pageNum, pageSize);
    }
}
