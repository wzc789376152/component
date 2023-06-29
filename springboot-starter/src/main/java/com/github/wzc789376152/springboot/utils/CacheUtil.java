package com.github.wzc789376152.springboot.utils;



import com.github.wzc789376152.springboot.cache.CacheEnumInterface;
import com.github.wzc789376152.springboot.cache.ICacheService;
import com.github.wzc789376152.springboot.config.SpringContextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据缓存
 *
 * @author : 梁维锟
 * @since : 2022/6/7
 */
public class CacheUtil {
    private static ICacheService getCacheService() {
        return SpringContextUtil.getBean(ICacheService.class);
    }

    /**
     * 采用内存、redis二级缓存，内存缓存保存10秒，redis缓存定时更新
     *
     * @param cacheEnum 缓存类型
     * @param key 缓存key
     * @param tClass 缓存类
     * @param <T> T
     * @return T
     */
    protected static <T> T getCache(CacheEnumInterface cacheEnum, String key, Class<T> tClass) {
        Object obj = CurrentHashMapUtil.get(cacheEnum.getKey() + "_" + key);
        if (obj != null) {
            return (T) obj;
        }
        T cacheData = getCacheService().getCache(cacheEnum, key, tClass);
        if (cacheData != null) {
            CurrentHashMapUtil.put(cacheEnum.getKey() + "_" + key, cacheData);
        }
        return cacheData;
    }

    protected static <T> List<T> getCache(CacheEnumInterface cacheEnum, Class<T> tClass) {
        Object obj = CurrentHashMapUtil.get(cacheEnum.getKey() + "_list");
        if (obj != null) {
            return (List<T>) obj;
        }
        Map<String, T> map = getCacheService().getCacheMap(cacheEnum, tClass);
        if (map != null) {
            List<T> list = new ArrayList<>(map.values());
            CurrentHashMapUtil.put(cacheEnum.getKey() + "_list", list);
            return list;
        }
        return new ArrayList<>();
    }

}
