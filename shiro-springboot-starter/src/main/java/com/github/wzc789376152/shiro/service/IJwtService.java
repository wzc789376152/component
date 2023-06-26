package com.github.wzc789376152.shiro.service;

import com.github.wzc789376152.shiro.realm.UserInfo;
import com.github.wzc789376152.shiro.token.JwtTokenResult;

public interface IJwtService {
    /**
     * 创建token
     *
     * @return string
     */
    JwtTokenResult createToken(UserInfo userInfo);

    Boolean verify(String token);

    JwtTokenResult refresh(String refreshToken);
}
