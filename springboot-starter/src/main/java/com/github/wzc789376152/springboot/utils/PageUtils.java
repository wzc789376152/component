package com.github.wzc789376152.springboot.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReflectUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.wzc789376152.springboot.config.SpringContextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class PageUtils {
    private final Integer pageNum;
    private final Integer pageSize;

    private final ExecutorService executorService;

    private Boolean count = true;
    private Page page;

    @FunctionalInterface
    public interface PageFunction {
        void list();
    }

    @FunctionalInterface
    public interface PageConvertTwoFunction<T1, T2, R> {
        R convert(T1 t1, T2 t2);
    }

    @FunctionalInterface
    public interface PageConvertFunction<T, R> {
        R convert(T object);
    }

    public PageUtils(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        if (pageSize == 0) {
            this.count = false;
        }
        executorService = SpringContextUtil.getBean("pageResultAsync", ExecutorService.class);
    }

    public PageUtils count(Boolean count) {
        this.count = count;
        return this;
    }

    public static PageUtils page(Integer pageNum, Integer pageSize) {
        return new PageUtils(pageNum, pageSize);
    }

    public PageUtils start(PageFunction pageFunction) {
        page = PageHelper.startPage(pageNum, pageSize, count, null, pageSize == 0);
        try {
            pageFunction.list();
        } finally {
            PageHelper.clearPage();
        }
        return this;
    }

    public <T> PageInfo<T> result(Class<T> tClass) {
        Page<T> toPage = page(tClass);
        return PageInfo.of(toPage);
    }

    private <T> Page<T> page(Class<T> tClass) {
        Page<T> toPage = new Page<>(page.getPageNum(), page.getPageSize());
        toPage.setPages(page.getPages());
        toPage.setTotal(page.getTotal());
        for (Object from : page.getResult()) {
            if (from.getClass().getName().equals(tClass.getName())) {
                toPage.add((T) from);
            } else {
                T toObj = ReflectUtil.newInstance(tClass);
                Map<String, Object> stringBeanMap = BeanUtil.beanToMap(from);
                if (stringBeanMap != null) {
                    T toInstance = BeanUtil.fillBeanWithMapIgnoreCase(stringBeanMap, toObj, true);
                    if (toInstance != null) {
                        toPage.add(toInstance);
                    }
                }
            }
        }
        return toPage;
    }

    public <T> PageUtils resultThen(Class<T> tClass) {
        page = page(tClass);
        return this;
    }

    public <S, T> PageInfo<T> result(PageConvertFunction<S, T> pageConvertFunction) {
        Page<T> toPage = new Page<>(page.getPageNum(), page.getPageSize());
        toPage.setPages(page.getPages());
        toPage.setTotal(page.getTotal());
        List<Future<T>> futureList = new ArrayList<>();
        for (Object from : page.getResult()) {
            futureList.add(executorService.submit(() -> pageConvertFunction.convert((S) from)));
        }
        for (Future<T> future : futureList) {
            try {
                toPage.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return PageInfo.of(toPage);
    }

    public <S, P, T> PageInfo<T> result(PageConvertTwoFunction<S, P, T> pageConvertFunction, P param) {
        Page<T> toPage = new Page<>(page.getPageNum(), page.getPageSize());
        toPage.setPages(page.getPages());
        toPage.setTotal(page.getTotal());
        List<Future<T>> futureList = new ArrayList<>();
        for (Object from : page.getResult()) {
            futureList.add(executorService.submit(() -> pageConvertFunction.convert((S) from, param)));
        }
        for (Future<T> future : futureList) {
            try {
                toPage.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return PageInfo.of(toPage);
    }
}
