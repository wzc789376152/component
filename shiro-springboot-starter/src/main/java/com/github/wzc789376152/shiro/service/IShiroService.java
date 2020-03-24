package com.github.wzc789376152.shiro.service;

import java.util.List;

public interface IShiroService {
    String findPasswordByUsername(String username);

    String findSaltByUsername(String username);

    List<String> findRolesByUsername(String username);

    List<String> findPermissionsByUsername(String username);
}
