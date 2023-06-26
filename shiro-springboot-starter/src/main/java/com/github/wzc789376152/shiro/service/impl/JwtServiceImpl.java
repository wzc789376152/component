package com.github.wzc789376152.shiro.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.realm.UserInfo;
import com.github.wzc789376152.shiro.service.IJwtService;
import com.github.wzc789376152.shiro.token.JwtTokenResult;

import java.util.Date;

public class JwtServiceImpl implements IJwtService {
    public JwtServiceImpl(ShiroJwtProperty shiroJwtProperty) {
        this.shiroJwtProperty = shiroJwtProperty;
    }

    private final ShiroJwtProperty shiroJwtProperty;

    @Override
    public JwtTokenResult createToken(UserInfo userInfo) {
        Date start = new Date();
        Date end = new Date(System.currentTimeMillis() + shiroJwtProperty.getTimeout());
        Date refreshEnd = new Date(System.currentTimeMillis() + shiroJwtProperty.getRefreshTimeout());
        Algorithm algorithm = Algorithm.HMAC256(shiroJwtProperty.getSecret());
        String token = JWT.create().withClaim("userInfo", JSONObject.toJSONString(userInfo)).withIssuedAt(start).withExpiresAt(end).sign(algorithm);
        String refreshToken = JWT.create().withClaim("userInfo", JSONObject.toJSONString(userInfo)).withIssuedAt(start).withExpiresAt(refreshEnd).sign(algorithm);
        JwtTokenResult jwtTokenResult = new JwtTokenResult();
        jwtTokenResult.setToken(token);
        jwtTokenResult.setRefreshToken(refreshToken);
        jwtTokenResult.setExpiresAt(end);
        return jwtTokenResult;
    }

    @Override
    public Boolean verify(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(shiroJwtProperty.getSecret());
            JWTVerifier verifier = JWT.require(algorithm).build();
            // 效验TOKEN
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException exception) {
            return false;
        }
    }

    @Override
    public JwtTokenResult refresh(String refreshToken) {
        if (verify(refreshToken)) {
            Date start = new Date();
            Date end = new Date(System.currentTimeMillis() + shiroJwtProperty.getTimeout());
            Algorithm algorithm = Algorithm.HMAC256(shiroJwtProperty.getSecret());
            DecodedJWT jwt = JWT.decode(refreshToken);
            String userInfo = jwt.getClaim("userInfo").asString();
            String token = JWT.create().withClaim("userInfo", userInfo).withIssuedAt(start).withExpiresAt(end).sign(algorithm);
            JwtTokenResult jwtTokenResult = new JwtTokenResult();
            jwtTokenResult.setToken(token);
            jwtTokenResult.setRefreshToken(refreshToken);
            jwtTokenResult.setExpiresAt(end);
            return jwtTokenResult;
        } else {
            throw new TokenExpiredException("token已失效");
        }
    }
}
