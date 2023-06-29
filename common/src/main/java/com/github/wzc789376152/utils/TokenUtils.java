package com.github.wzc789376152.utils;


import com.alibaba.ttl.TransmittableThreadLocal;
import com.github.wzc789376152.vo.UserInfo;

import java.io.Serializable;

/**
 * 获取当前登录用户
 */
public class TokenUtils {
    private final static TransmittableThreadLocal<UserInfo> context = new TransmittableThreadLocal<>();

    public static UserInfo getCurrentUser() {
        return context.get();
    }

    public static Serializable getCurrentUserId() {
        UserInfo userInfo = context.get();
        if (userInfo != null) {
            return userInfo.getId();
        }
        return null;
    }

    public static void setUserInfo(UserInfo userInfo) {
        if (userInfo != null) {
            context.set(userInfo);
        }
    }

    public static void remove() {
        context.remove();
    }
}

