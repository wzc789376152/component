package com.github.wzc789376152.file.utils;

import java.util.HashMap;
import java.util.Map;

public class FileSyncUtils {
    private static Map<Object, Object> map = new HashMap<Object, Object>();

    public static synchronized Object getObject(Object key) {
        Object object = map.get(key);
        if (object == null) {
            object = new Object();
            map.put(key, object);
        }
        return object;
    }
}
