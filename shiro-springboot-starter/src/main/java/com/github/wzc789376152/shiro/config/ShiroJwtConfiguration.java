package com.github.wzc789376152.shiro.config;

import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.properties.ShiroRedisProperty;
import com.github.wzc789376152.shiro.realm.ShiroJwtRealm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.shiro.jwt", name = "enable", havingValue = "true")
@EnableConfigurationProperties(ShiroJwtProperty.class)
public class ShiroJwtConfiguration {
    @Bean("jwtRealm")
    public ShiroJwtRealm realm() {
        return new ShiroJwtRealm();
    }
}
