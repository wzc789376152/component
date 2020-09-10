package com.github.wzc789376152.generator.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wzc789376152.generator.common.service.IBaseService;

import java.io.Serializable;

public class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements IBaseService<T> {

    public <P> IPage<P> selectPage(int currentPage, int pageSize, Wrapper<P> queryWrapper) {
        IPage<P> page = new Page<P>(currentPage, pageSize);
        page = ((BaseMapper) getBaseMapper()).selectPage(page, queryWrapper);
        return page;
    }

    public <D> D selectDetailById(Serializable id) {
        return (D) ((BaseMapper) getBaseMapper()).selectById(id);
    }
}
