package com.github.wzc789376152.springboot.cache.impl;


import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.wzc789376152.springboot.cache.CacheEnumInterface;
import com.github.wzc789376152.springboot.cache.ICacheService;
import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.github.wzc789376152.springboot.config.redis.IRedisService;
import com.github.wzc789376152.utils.JSONUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class CacheServiceImpl implements ICacheService {

    protected abstract String getRedisKey();

    private IRedisService getRedisService() {
        return SpringContextUtil.getBean(IRedisService.class);
    }

    @Override
    public Boolean initCache(CacheEnumInterface cacheEnum) {
        return true;
    }

    protected <R, T> void putRedis(CacheEnumInterface cacheEnum, SFunction<R, ?> sFunction, List<T> dataList, Class<T> tClass) {
        if (dataList == null || dataList.size() == 0) {
            return;
        }
        try {
            String methodName = LambdaUtils.extract(sFunction).getImplMethodName();
            Method getMethod = tClass.getDeclaredMethod(methodName);
            Map<String, T> map = new HashMap<>();
            for (T data : dataList) {
                map.put(getMethod.invoke(data).toString(), data);
            }
            getRedisService().setCacheMap(getRedisKey() + cacheEnum.getKey(), map);
            getRedisService().expire(getRedisKey() + cacheEnum.getKey(), 10L, TimeUnit.MINUTES);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Object getCache(CacheEnumInterface cacheEnum, String key);

    @Override
    public <T> T getCache(CacheEnumInterface cacheEnum, String key, Class<T> tClass) {
        T value = getRedisService().getCacheMapValue(getRedisKey() + cacheEnum.getKey(), key, tClass);
        if (value != null) {
            return value;
        }
        value = JSONUtils.parse(getCache(cacheEnum, key), tClass);
        if (value != null) {
            getRedisService().setCacheMapValue(getRedisKey() + cacheEnum.getKey(), key, value);
            getRedisService().expire(getRedisKey() + cacheEnum.getKey(), 5L, TimeUnit.MINUTES);
        }
        return value;
    }

    @Override
    public <T> Map<String, T> getCacheMap(CacheEnumInterface cacheEnum, Class<T> tClass) {
        Map<String, T> map = getRedisService().getCacheMap(getRedisKey() + cacheEnum.getKey(), tClass);
        if (map != null) {
            return map;
        }
        return null;
    }
}
