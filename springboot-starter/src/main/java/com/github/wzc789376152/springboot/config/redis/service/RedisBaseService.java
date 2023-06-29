package com.github.wzc789376152.springboot.config.redis.service;

import org.springframework.data.redis.core.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * spring redis 工具类
 *
 * @author jackw
 **/
public class RedisBaseService {

    private RedisTemplate redisTemplate;

    public RedisBaseService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Long  increment(final String key, long delta) {
        return redisTemplate.opsForValue().increment(key,delta);
    }


    public Long  decrement(final String key, long delta) {
        return redisTemplate.opsForValue().decrement(key,delta);
    }

    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }


    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }


    public <T> Boolean setNx(final String key, final T value, final Long timeout, final TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
    }


    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }


    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }


    public long getExpire(final String key) {
        Long expire = redisTemplate.getExpire(key);
        return expire == null ? 0 : expire;
    }


    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }


    public <T> T getCacheObject(final String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }


    public boolean deleteObject(final String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }


    public long deleteObject(final Collection collection) {
        Long result = redisTemplate.delete(collection);
        return result == null ? 0 : result;
    }


    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }


    public <T> List<T> getCacheList(final String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }


    public <T> long cachePush(final String key, final T value) {
        Long count = redisTemplate.opsForList().rightPush(key, value);
        return count == null ? 0 : count;
    }


    public <T>  T cacheProp(final String key,long timeout, TimeUnit unit) {
        ListOperations<String, T> operation = redisTemplate.opsForList();
        return operation.leftPop(key,timeout,unit);
    }

    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext()) {
            setOperation.add(it.next());
        }
        return setOperation;
    }


    public <T> Set<T> getCacheSet(final String key) {
        return redisTemplate.opsForSet().members(key);
    }


    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }


    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }


    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    public Long increment(String key, String hashKey, Long delate) {
        return redisTemplate.opsForHash().increment(key, hashKey, delate);
    }
}
