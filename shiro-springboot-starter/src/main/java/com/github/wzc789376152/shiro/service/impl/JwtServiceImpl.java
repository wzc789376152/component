package com.github.wzc789376152.shiro.service.impl;

import cn.hutool.core.codec.Base64Encoder;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.service.IJwtService;
import com.github.wzc789376152.shiro.token.JwtTokenResult;
import com.github.wzc789376152.utils.JSONUtils;
import com.github.wzc789376152.vo.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            String host = request.getHeader("x-forwarded-host");
            HttpServletResponse response = requestAttributes.getResponse();
            if (response != null) {
                Cookie tokenCookie = new Cookie("jwt-token", jwtTokenResult.getToken());
                tokenCookie.setMaxAge((int) (jwtTokenResult.getExpiresAt().getTime() - new Date().getTime()) / 1000);
                tokenCookie.setPath("/");
                Cookie refreshCookie = new Cookie("jwt-refreshToken", jwtTokenResult.getRefreshToken());
                refreshCookie.setMaxAge((int) (refreshEnd.getTime() - new Date().getTime()) / 1000);
                refreshCookie.setPath("/");
                if (StringUtils.isNotEmpty(host)) {
                    host = getHost(host);
                    tokenCookie.setDomain(host);
                    refreshCookie.setDomain(host);
                }
                response.addCookie(tokenCookie);
                response.addCookie(refreshCookie);
            }
        }
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
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String host = request.getHeader("x-forwarded-host");
                HttpServletResponse response = requestAttributes.getResponse();
                if (response != null) {
                    Cookie tokenCookie = new Cookie("jwt-token", jwtTokenResult.getToken());
                    tokenCookie.setMaxAge((int) (jwtTokenResult.getExpiresAt().getTime() - new Date().getTime()) / 1000);
                    tokenCookie.setPath("/");
                    if (StringUtils.isNotEmpty(host)) {
                        tokenCookie.setDomain(getHost(host));
                    }
                    response.addCookie(tokenCookie);
                }
            }
            return jwtTokenResult;
        } else {
            throw new TokenExpiredException("token已失效");
        }
    }

    @Override
    public Boolean removeToken() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            String host = request.getHeader("x-forwarded-host");
            HttpServletResponse response = requestAttributes.getResponse();
            if (response != null) {
                Cookie tokenCookie = new Cookie("jwt-token", null);
                tokenCookie.setMaxAge(0);
                tokenCookie.setPath("/");
                Cookie refreshCookie = new Cookie("jwt-refreshToken", null);
                refreshCookie.setMaxAge(0);
                refreshCookie.setPath("/");
                if (StringUtils.isNotEmpty(host)) {
                    host = getHost(host);
                    tokenCookie.setDomain(host);
                    refreshCookie.setDomain(host);
                }
                response.addCookie(tokenCookie);
                response.addCookie(refreshCookie);
            }
        }
        return true;
    }

    @Override
    public UserInfo getUserInfo(String token) {
        DecodedJWT jwt = JWT.decode(token);
        String userInfo = jwt.getClaim("userInfo").asString();
        return JSONUtils.parse(userInfo, UserInfo.class);
    }


    private String getHost(String host) {
        host = host.replaceAll("https://", "");
        host = host.replaceAll("http://", "");
        host = host.split(":")[0];
        String[] hostArray = host.split("\\.");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < hostArray.length; i++) {
            if (i > 0) {
                result.append(".").append(hostArray[i]);
            }
        }
        if (StringUtils.isEmpty(result.toString())) {
            result = new StringBuilder(host);
        }
        return result.toString();
    }
}
