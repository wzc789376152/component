package com.github.wzc789376152.shiro.service;

import com.github.wzc789376152.shiro.token.JwtTokenResult;
import com.github.wzc789376152.vo.UserInfo;

import javax.servlet.http.HttpServletResponse;

public interface IJwtService {
    /**
     * 创建token
     *
     * @return string
     */
    JwtTokenResult createToken(UserInfo userInfo);

    JwtTokenResult createToken(UserInfo userInfo, HttpServletResponse response);

    Boolean verify(String token);

    JwtTokenResult refresh(String refreshToken);

    JwtTokenResult refresh(String refreshToken, HttpServletResponse response);

    UserInfo getUserInfo(String token);
}
