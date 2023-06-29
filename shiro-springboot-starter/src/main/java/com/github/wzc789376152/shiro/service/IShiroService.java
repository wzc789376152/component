package com.github.wzc789376152.shiro.service;

import com.github.wzc789376152.vo.UserInfo;

import java.util.List;

public interface IShiroService {
    UserInfo findUserInfoByUsername(String username, String host);

    String findPasswordByUsername(String username,String host);

    String findSaltByUsername(String username,String host);

    List<String> findRolesByObject(UserInfo object);

    List<String> findPermissionsByObject(UserInfo object);

}
