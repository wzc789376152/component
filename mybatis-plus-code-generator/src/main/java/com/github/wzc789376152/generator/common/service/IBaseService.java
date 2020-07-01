package com.github.wzc789376152.generator.common.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;

public interface IBaseService<T> extends IService<T> {
    /**
     * 分页条件查询
     *
     * @param currentPage 页码
     * @param pageSize    页大小
     * @return
     */
    <P> IPage<P> selectPage(int currentPage, int pageSize, Wrapper<P> queryWrapper);

    /**
     * 查询详情
     *
     * @param id
     * @return
     */

    <D> D selectDetailById(Serializable id);
}
