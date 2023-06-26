package com.github.wzc789376152.shiro.service;

public interface IShiroCodeService {
    String get(String username,String host);

    Boolean save(String username,String host, String salt, String code);
}
