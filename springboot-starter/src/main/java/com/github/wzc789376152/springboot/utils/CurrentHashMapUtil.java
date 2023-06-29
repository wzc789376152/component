package com.github.wzc789376152.springboot.utils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 内存缓存对象
 */
@Slf4j
public class CurrentHashMapUtil {
    /**
     * 线程池
     */
    private static final ScheduledExecutorService executorService;

    /**
     * 前缀
     */
    private static final String THREAD_POOL_NAME_PREFIX = "delete_expire_cache_";

    static {
        executorService = new ScheduledThreadPoolExecutor(1,
                ThreadFactoryBuilder.create()
                        .setNamePrefix(THREAD_POOL_NAME_PREFIX)
                        .build());
        executorService.scheduleWithFixedDelay(() -> {
            try {
                Map<String, Object> mapCache = CurrentHashMapUtil.CACHE_MAP;
                if (MapUtil.isEmpty(mapCache)) {
                    return;
                }
                for (String key : mapCache.keySet()) {
                    CurrentHashMapUtil.checkCacheName(key);
                }
            } catch (Exception exception) {
                log.error("清理过期缓存异常", exception);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 预缓存信息
     */
    private static final Map<String, Object> CACHE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Long> CACHE_MAP_TIME = new ConcurrentHashMap<>();

    /**
     * 每个缓存生效时间10秒
     */
    public static final long CACHE_HOLD_TIME = 10 * 1000L;

    /**
     * 存放一个缓存对象，默认保存时间10s
     *
     * @param cacheName 缓存名称
     * @param obj 缓存值
     */
    public static void put(String cacheName, Object obj) {
        put(cacheName, obj, CACHE_HOLD_TIME);
    }

    /**
     * 存放一个缓存对象，保存时间为holdTime
     *
     * @param cacheName 缓存名称
     * @param obj 缓存值
     * @param holdTime 过期时间
     */
    public static void put(String cacheName, Object obj, long holdTime) {
        if (checkCacheName(cacheName)) {
            return;
        }
        CACHE_MAP.put(cacheName, obj);
        CACHE_MAP_TIME.put(cacheName, System.currentTimeMillis() + holdTime);
    }

    /**
     * 取出一个缓存对象
     *
     * @param cacheName 缓存名称
     * @return Object
     */
    public static Object get(String cacheName) {
        if (checkCacheName(cacheName)) {
            return CACHE_MAP.get(cacheName);
        }
        return null;
    }

    /**
     * 删除所有缓存
     */
    public static void removeAll() {
        CACHE_MAP.clear();
        CACHE_MAP_TIME.clear();
    }

    /**
     * 删除某个缓存
     *
     * @param cacheName 缓存名称
     */
    public static void remove(String cacheName) {
        CACHE_MAP.remove(cacheName);
        CACHE_MAP_TIME.remove(cacheName);
    }

    /**
     * 检查缓存对象是否存在，
     * 若不存在，则返回false
     * 若存在，检查其是否已过有效期，如果已经过了则删除该缓存并返回false
     *
     * @param cacheName 缓存名称
     * @return boolean
     */
    public static boolean checkCacheName(String cacheName) {
        Long cacheHoldTime = CACHE_MAP_TIME.get(cacheName);
        if (cacheHoldTime == null || cacheHoldTime == 0L) {
            return false;
        }
        if (cacheHoldTime < System.currentTimeMillis()) {
            remove(cacheName);
            return false;
        }
        return true;
    }
}
