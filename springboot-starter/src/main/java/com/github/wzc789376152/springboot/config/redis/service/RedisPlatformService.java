package com.github.wzc789376152.springboot.config.redis.service;



import com.github.wzc789376152.springboot.config.redis.IRedisService;
import com.github.wzc789376152.utils.JSONUtils;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

/**
 * spring redis 工具类
 *
 * @author jackw
 **/
public class RedisPlatformService extends RedisBaseService implements IRedisService {

    private RedisTemplate redisTemplate;

    public RedisPlatformService(RedisTemplate redisTemplate) {
        super(redisTemplate);
        this.redisTemplate = redisTemplate;
    }

    public <T> T getCacheObject(final String key, Class<T> tClass) {
        ValueOperations<String, Object> operation = redisTemplate.opsForValue();
        Object result = operation.get(key);
        if (result != null) {
            return JSONUtils.parse(result, tClass);
        }
        return null;
    }

    public <T> List<T> getCacheObjectList(final String key, Class<T> tClass) {
        ValueOperations<String, Object> operation = redisTemplate.opsForValue();
        Object result = operation.get(key);
        if (result != null) {
            return JSONUtils.parseArray(result, tClass);
        }
        return null;
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    public <T> List<T> getCacheList(final String key, Class<T> tClass) {
        List<Object> jsonObjectList = getCacheList(key);
        if (jsonObjectList == null) {
            return null;
        }
        return JSONUtils.parseArray(jsonObjectList, tClass);
    }

    public <T> long removeCacheList(final String key, final List<T> dataList) {
        long count = 0;
        for (T value : dataList) {
            count += redisTemplate.opsForList().remove(key, 1, value);
        }
        return count;
    }

    public <T> long removeCacheList(final String key, final T value) {
        return redisTemplate.opsForList().remove(key, 1, value);
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext()) {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    public <T> Set<T> getCacheSet(final String key, Class<T> tClass) {
        Set<Object> setObject = getCacheSet(key);
        if (setObject == null) {
            return null;
        }
        return new HashSet<>(JSONUtils.parseArray(setObject, tClass));
    }

    public <T> long removeCacheSet(final String key, final T data) {
        SetOperations<String, T> setOperation = redisTemplate.opsForSet();
        if (data != null && key != null) {
            return setOperation.remove(key, data);
        }
        return 0L;
    }

    public <T> long removeCacheSet(final String key, final Set<T> dataSet) {
        SetOperations<String, T> setOperation = redisTemplate.opsForSet();
        if (dataSet != null && key != null) {
            return setOperation.remove(key, dataSet.toArray());
        }
        return 0L;
    }

    /**
     * 缓存Map
     *
     * @param key key
     * @param dataMap dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key key
     * @return Map
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public <T> Map<String, T> getCacheMap(final String key, Class<T> tClass) {
        Map<String, Object> mapObj = getCacheMap(key);
        if (mapObj == null) {
            return null;
        }
        Map<String, T> map = new HashMap<>();
        for (String mapKey : mapObj.keySet()) {
            map.put(mapKey, JSONUtils.parse(mapObj.get(mapKey), tClass));
        }
        return map;
    }

    public <T> long removeCacheMap(final String key, Map<String, T> dataMap) {
        return redisTemplate.opsForHash().delete(key, dataMap.keySet().toArray());
    }

    public long removeCacheMap(final String key, final String mapKey) {
        return redisTemplate.opsForHash().delete(key, mapKey);
    }

    @Override
    public <T> T getCacheMapValue(String key, String hKey, Class<T> tClass) {
        Object object = this.getCacheMapValue(key, hKey);
        return JSONUtils.parse(object, tClass);
    }
}
