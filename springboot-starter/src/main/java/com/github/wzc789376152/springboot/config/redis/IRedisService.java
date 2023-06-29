package com.github.wzc789376152.springboot.config.redis;

import org.springframework.data.redis.core.BoundSetOperations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface IRedisService {
    <T> void setCacheObject(final String key, final T value);
    <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit);
    <T> Boolean setNx(final String key, final T value, final Long timeout, final TimeUnit timeUnit);
    boolean expire(final String key, final long timeout);
    boolean expire(final String key, final long timeout, final TimeUnit unit);
    long getExpire(final String key);
    Boolean hasKey(String key);

    <T> T getCacheObject(final String key);
    <T> T getCacheObject(final String key, Class<T> tClass);
    <T> List<T> getCacheObjectList(final String key, Class<T> tClass);
    boolean deleteObject(final String key);
    long deleteObject(final Collection collection);
    <T> long setCacheList(final String key, final List<T> dataList);
    <T> List<T> getCacheList(final String key);
    <T> List<T> getCacheList(final String key, Class<T> tClass);
    <T> long removeCacheList(final String key, final List<T> dataList);
    <T> long removeCacheList(final String key, final T value);

    <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet);
    <T> Set<T> getCacheSet(final String key);
    <T> Set<T> getCacheSet(final String key, Class<T> tClass);
    <T> long removeCacheSet(final String key, final T data);
    <T> long removeCacheSet(final String key, final Set<T> dataSet);
    <T> void setCacheMap(final String key, final Map<String, T> dataMap);
    <T> Map<String, T> getCacheMap(final String key);
    <T> Map<String, T> getCacheMap(final String key, Class<T> tClass);
    <T> long removeCacheMap(final String key, Map<String, T> dataMap);
    long removeCacheMap(final String key, final String mapKey);
    <T> void setCacheMapValue(final String key, final String hKey, final T value);
    <T> T getCacheMapValue(final String key, final String hKey);
    <T> T getCacheMapValue(final String key, final String hKey,Class<T> tClass);
    <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys);
    Collection<String> keys(final String pattern);
    Long increment(String key, String hashKey, Long delate);
}
