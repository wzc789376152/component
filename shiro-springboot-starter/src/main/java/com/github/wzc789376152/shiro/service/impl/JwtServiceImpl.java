package com.github.wzc789376152.shiro.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.service.IJwtService;
import org.crazycake.shiro.RedisManager;

import java.util.Date;
import java.util.Set;

public class JwtServiceImpl implements IJwtService {
    public JwtServiceImpl(ShiroJwtProperty shiroJwtProperty, RedisManager redisManager) {
        this.shiroJwtProperty = shiroJwtProperty;
        this.redisManager = redisManager;
    }

    private final ShiroJwtProperty shiroJwtProperty;
    private final RedisManager redisManager;
    private final String prefix = "shiro:jwt:";

    @Override
    public String createToken(String username, String secret) {
        if (!shiroJwtProperty.getMultipleLogin()) {
            multipleLoginOut(username);
        }
        Date start = new Date();
        Date end = new Date(System.currentTimeMillis() + shiroJwtProperty.getTimeout());
        Algorithm algorithm = Algorithm.HMAC256(secret);
        String token = JWT.create().withClaim("username", username).withIssuedAt(start).withExpiresAt(end).sign(algorithm);
        if (redisManager != null) {
            redisManager.set((prefix + token).getBytes(), username.getBytes(), redisManager.getTimeout());
        }
        return token;
    }

    @Override
    public Boolean verify(String token, String username, String secret) {
        try {
            if (redisManager != null) {
                byte[] value = redisManager.get((prefix + token).getBytes());
                if (value == null) {
                    // 根据密码生成JWT效验器
                    return false;
                }
            }
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).withClaim("username", username).build();
            // 效验TOKEN
            verifier.verify(token);
            if (redisManager != null) {
                redisManager.set((prefix + token).getBytes(), username.getBytes(), redisManager.getTimeout());
            }
            return true;
        } catch (JWTVerificationException exception) {
            return false;
        }
    }

    @Override
    public Boolean cancel(String token) {
        if (redisManager != null) {
            redisManager.del((prefix + token).getBytes());
        }
        return true;
    }

    @Override
    public String getUserName(String token) {
        try {
            if (redisManager != null) {
                byte[] result = redisManager.get((prefix + token).getBytes());
                if (result == null) {
                    return null;
                }
            }
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("username").asString();
        } catch (JWTDecodeException e) {
            return null;
        }
    }

    private void multipleLoginOut(String username) {
        if (redisManager != null) {
            Set<byte[]> keys = redisManager.keys((prefix + "*").getBytes());
            for (byte[] key : keys) {
                byte[] valueByte = redisManager.get(key);
                String value = new String(valueByte);
                if (username.equals(value)) {
                    redisManager.del(key);
                }
            }
        }
    }
}
