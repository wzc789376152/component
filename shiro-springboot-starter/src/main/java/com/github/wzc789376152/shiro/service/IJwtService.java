package com.github.wzc789376152.shiro.service;

public interface IJwtService {
    /**
     * 创建token
     *
     * @param username
     * @param secret
     * @return
     */
    String createToken(String username, String secret);

    Boolean verify(String token, String username, String secret);

    Boolean cancel(String token);

    String getUserName(String token);
}
