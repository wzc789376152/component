package com.github.wzc789376152.shiro.service;

public interface IShiroCodeService {
    String get(String username);

    Boolean save(String username, String salt, String code);
}
