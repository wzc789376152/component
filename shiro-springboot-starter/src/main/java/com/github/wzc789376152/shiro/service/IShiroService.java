package com.github.wzc789376152.shiro.service;

import com.github.wzc789376152.shiro.realm.UserInfo;

import java.util.List;

public interface IShiroService {
    UserInfo findUserInfoByUsername(String use0rname);

    String findPasswordByUsername(String username);

    String findSaltByUsername(String username);

    List<String> findRolesByObject(UserInfo object);

    List<String> findPermissionsByObject(UserInfo object);
}
