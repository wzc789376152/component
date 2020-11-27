package com.github.wzc789376152.shiro.service;

import java.util.List;

public interface IShiroService<T extends Object> {
    T findUserInfoByUsername(String username);

    String findPasswordByUsername(String username);

    String findSaltByUsername(String username);

    List<String> findRolesByObject(T object);

    List<String> findPermissionsByObject(T object);
}
