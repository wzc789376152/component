package com.github.wzc789376152.springboot.shardingjdbc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface IShardingService<T> {
    Integer queryCount(QueryWrapper<?> wrapper) throws ExecutionException, InterruptedException;

    Future<Integer> queryCountAsync(QueryWrapper<?> wrapper);

    List<T> queryList(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) throws ExecutionException, InterruptedException;

    Future<List<T>> queryListAsync(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize);

    PageInfo<T> queryPage(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) throws ExecutionException, InterruptedException;
}