package com.github.wzc789376152.springboot.cache;

import java.util.Map;

public interface ICacheService {
    Boolean initCache(CacheEnumInterface cacheEnum);

    <T> T getCache(CacheEnumInterface cacheEnum, String key, Class<T> tClass);

    <T> Map<String, T> getCacheMap(CacheEnumInterface cacheEnum,Class<T> tClass);
}
