package com.github.wzc789376152.shiro.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.github.wzc789376152.shiro.realm.UserInfo;
import org.apache.shiro.SecurityUtils;

import java.io.Serializable;

/**
 * 获取当前登录用户
 */
public class TokenUtils {
    private final static TransmittableThreadLocal<UserInfo> context = new TransmittableThreadLocal<>();

    public static UserInfo getCurrentUser() {
        UserInfo userInfo = context.get();
        if (userInfo == null) {
            userInfo = getUserInfo();
            if (userInfo != null) {
                context.set(userInfo);
            }
        }
        return userInfo;
    }

    public static Serializable getCurrentUserId() {
        UserInfo userInfo = context.get();
        if (userInfo == null) {
            userInfo = getUserInfo();
            if (userInfo != null) {
                context.set(userInfo);
            }
        }
        if (userInfo != null) {
            return userInfo.getId();
        }
        return null;
    }

    public static <T extends UserInfo> void setUserInfo(T userInfo) {
        if (userInfo != null) {
            context.set(userInfo);
        }
    }

    public static void remove() {
        context.remove();
    }

    private static UserInfo getUserInfo() {
        if (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().getPrincipal() != null) {
            Object userInfo = SecurityUtils.getSubject().getPrincipal();
            return (UserInfo) userInfo;
        }
        return null;
    }
}

