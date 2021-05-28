package com.github.wzc789376152.shiro.config;

import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.properties.ShiroRedisProperty;
import com.github.wzc789376152.shiro.realm.ShiroJwtRealm;
import com.github.wzc789376152.shiro.service.IJwtService;
import com.github.wzc789376152.shiro.service.IShiroService;
import com.github.wzc789376152.shiro.service.impl.JwtServiceImpl;
import org.crazycake.shiro.RedisManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnProperty(prefix = "spring.shiro.jwt", name = "enable", havingValue = "true")
@EnableConfigurationProperties(ShiroJwtProperty.class)
@ConditionalOnClass(IJwtService.class)
public class ShiroJwtConfiguration {
    @Autowired
    @Lazy
    IJwtService jwtService;
    @Autowired
    @Lazy
    IShiroService shiroService;
    @Autowired
    private ShiroJwtProperty shiroJwtProperty;
    @Autowired(required = false)
    @Lazy
    private RedisManager redisManager;

    @Bean("jwtRealm")
    public ShiroJwtRealm realm() {
        return new ShiroJwtRealm(jwtService, shiroService);
    }

    @Bean
    @ConditionalOnMissingBean(IJwtService.class)
    public IJwtService jwtService() {
        return new JwtServiceImpl(shiroJwtProperty,redisManager);
    }
}
