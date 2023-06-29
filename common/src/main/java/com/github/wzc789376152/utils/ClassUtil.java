package com.github.wzc789376152.utils;

import java.lang.reflect.Method;

/**
 * @author sunyu
 */
public class ClassUtil {

    /**
     * 反射获取方法
     *
     * @param clazz 类型
     * @param name 名称
     * @return method
     */
    public static Method getMethod(Class clazz, String name) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(name)) {
                return method;
            }
        }
        return null;
    }
}
