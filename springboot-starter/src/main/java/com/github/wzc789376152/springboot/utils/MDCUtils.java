package com.github.wzc789376152.springboot.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

public class MDCUtils {
    private final static TransmittableThreadLocal<Map<String, String>> context = new TransmittableThreadLocal<>();

    public static void put(String key, String mdc) {
        MDC.put(key, mdc);
        Map<String, String> map = context.get();
        if (map == null) {
            map = new HashMap<>();
            context.set(map);
        }
        map.put(key, mdc);
    }

    public static String get(String key) {
        Map<String, String> map = context.get();
        if (map == null) {
            map = new HashMap<>();
        }
        return map.get(key);
    }

    public static void remove(String key) {
        Map<String, String> map = context.get();
        if (map == null) {
            map = new HashMap<>();
        }
        map.remove(key);
    }
}
