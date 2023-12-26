package com.github.wzc789376152.shiro.service;

import com.github.wzc789376152.shiro.token.JwtTokenResult;
import com.github.wzc789376152.vo.UserInfo;

public interface IJwtService {
    /**
     * 创建token
     *
     * @return string
     */
    JwtTokenResult createToken(UserInfo userInfo);

    Boolean verify(String token);

    JwtTokenResult refresh(String refreshToken);

    UserInfo getUserInfo(String token);
}
