package com.github.wzc789376152.shiro.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.realm.UserInfo;
import com.github.wzc789376152.shiro.service.IJwtService;
import org.crazycake.shiro.RedisManager;

import java.util.Date;
import java.util.Set;

public class JwtServiceImpl implements IJwtService {
    public JwtServiceImpl(ShiroJwtProperty shiroJwtProperty) {
        this.shiroJwtProperty = shiroJwtProperty;
    }

    private final ShiroJwtProperty shiroJwtProperty;

    @Override
    public String createToken(UserInfo userInfo) {
        Date start = new Date();
        Date end = new Date(System.currentTimeMillis() + shiroJwtProperty.getTimeout());
        Algorithm algorithm = Algorithm.HMAC256(shiroJwtProperty.getSecret());
        return JWT.create().withClaim("userInfo", JSONObject.toJSONString(userInfo)).withIssuedAt(start).withExpiresAt(end).sign(algorithm);
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
}
