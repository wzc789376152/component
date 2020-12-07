package com.github.wzc789376152.shiro.config;

import com.github.wzc789376152.shiro.properties.ShiroCodeProperty;
import com.github.wzc789376152.shiro.realm.ShiroCodeRealm;
import com.github.wzc789376152.shiro.service.IJwtService;
import com.github.wzc789376152.shiro.service.IShiroCodeService;
import com.github.wzc789376152.shiro.service.impl.CodeServiceImpl;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShiroCodeProperty.class)
@ConditionalOnClass(IShiroCodeService.class)
public class ShiroCodeConfiguration {
    @Autowired(required = false)
    HashedCredentialsMatcher hashedCredentialsMatcher;

    @Bean("codeRealm")
    public ShiroCodeRealm shiroCodeRealm() {
        ShiroCodeRealm shiroCodeRealm = new ShiroCodeRealm();
        if (hashedCredentialsMatcher != null) {
            shiroCodeRealm.setCredentialsMatcher(hashedCredentialsMatcher);
        }
        return shiroCodeRealm;
    }

    @Bean
    @ConditionalOnMissingBean(IJwtService.class)
    public IShiroCodeService shiroCodeService() {
        return new CodeServiceImpl();
    }
}
