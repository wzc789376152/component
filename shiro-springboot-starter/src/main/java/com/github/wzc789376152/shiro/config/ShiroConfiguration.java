package com.github.wzc789376152.shiro.config;

import com.github.wzc789376152.shiro.exception.GlobalExceptionResolver;
import com.github.wzc789376152.shiro.filter.JwtFilter;
import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.properties.ShiroProperty;
import com.github.wzc789376152.shiro.properties.ShiroSessionProperty;
import com.github.wzc789376152.shiro.properties.ShiroUrlPer;
import com.github.wzc789376152.shiro.realm.ShiroCodeRealm;
import com.github.wzc789376152.shiro.realm.ShiroJwtRealm;
import com.github.wzc789376152.shiro.realm.ShiroPasswordRealm;
import com.github.wzc789376152.shiro.realm.UserModularRealmAuthenticator;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.annotation.Resource;
import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * shiro配置项
 */
@Configuration
@EnableConfigurationProperties(value = {ShiroProperty.class, ShiroSessionProperty.class, ShiroJwtProperty.class})
public class ShiroConfiguration {
    @Autowired
    private ShiroProperty shiroProperty;
    @Autowired(required = false)
    private ShiroSessionProperty shiroSessionProperty;
    @Autowired(required = false)
    private ShiroJwtProperty shiroJwtProperty;
    @Autowired(required = false)
    private CookieRememberMeManager cookieRememberMeManager;
    @Autowired(required = false)
    private DefaultSessionManager sessionManager;
    @Autowired(required = false)
    @Qualifier(value = "shiroRedisCacheManager")
    private CacheManager shiroRedisCacheManager;
    @Autowired(required = false)
    private ShiroJwtRealm shiroJwtRealm;
    @Autowired(required = false)
    private ShiroCodeRealm shiroCodeRealm;

    @Bean("passwordRealm")
    public ShiroPasswordRealm shiroRealm() {
        ShiroPasswordRealm realm = new ShiroPasswordRealm();
        HashedCredentialsMatcher hashedCredentialsMatcher = hashedCredentialsMatcher();
        if (hashedCredentialsMatcher != null) {
            realm.setCredentialsMatcher(hashedCredentialsMatcher);
        }
        return realm;
    }

    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher();
        hashedCredentialsMatcher.setHashAlgorithmName("md5");
        //散列的次数
        hashedCredentialsMatcher.setHashIterations(shiroProperty.getHashIterations());
        return hashedCredentialsMatcher;
    }

    @Bean
    public UserModularRealmAuthenticator userModularRealmAuthenticator() {
        // 自己重写的ModularRealmAuthenticator
        UserModularRealmAuthenticator modularRealmAuthenticator = new UserModularRealmAuthenticator();
        modularRealmAuthenticator.setAuthenticationStrategy(new AtLeastOneSuccessfulStrategy());
        return modularRealmAuthenticator;
    }

    @Bean(name = "securityManager")
    public DefaultWebSecurityManager securityManager(@Qualifier("userModularRealmAuthenticator") UserModularRealmAuthenticator userModularRealmAuthenticator) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        List<Realm> realms = new ArrayList<>();
        // 添加多个realm
        if (shiroJwtRealm != null) {
            securityManager.setAuthenticator(userModularRealmAuthenticator);
            realms.add(shiroJwtRealm);
        }
        if (shiroCodeRealm != null) {
            realms.add(shiroCodeRealm);
        }
        realms.add(shiroRealm());
        securityManager.setRealms(realms);
        if (shiroSessionProperty.getEnable()) {
            if (shiroRedisCacheManager != null) {
                securityManager.setCacheManager(shiroRedisCacheManager);
            }
            securityManager.setRememberMeManager(cookieRememberMeManager);
            securityManager.setSessionManager(sessionManager);
        } else {
            DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
            DefaultSessionStorageEvaluator defaultSessionStorageEvaluator = new DefaultSessionStorageEvaluator();
            defaultSessionStorageEvaluator.setSessionStorageEnabled(false);
            subjectDAO.setSessionStorageEvaluator(defaultSessionStorageEvaluator);
            securityManager.setSubjectDAO(subjectDAO);
        }
        return securityManager;
    }

    @Bean(name = "shiroFilter")
    public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        Map<String, String> filterChainDefinitionManager = new LinkedHashMap<String, String>();
        Map<String, Filter> filter = new LinkedHashMap<>(1);
        if (shiroJwtProperty != null && shiroJwtProperty.getEnable()) {
            filter.put("jwt", new JwtFilter(shiroJwtProperty, shiroProperty));
        }
        if (shiroProperty.getUrlPers() != null && shiroProperty.getUrlPers().size() > 0) {
            for (int i = 0; i < shiroProperty.getUrlPers().size(); i++) {
                ShiroUrlPer shiroUrlPer = shiroProperty.getUrlPers().get(i);
                filterChainDefinitionManager.put(shiroUrlPer.getUrl(), shiroUrlPer.getPer());
            }
        }
        shiroFilterFactoryBean.setFilters(filter);
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionManager);
        shiroFilterFactoryBean.setSuccessUrl(shiroProperty.getSuccessUrl());
        shiroFilterFactoryBean.setUnauthorizedUrl(shiroProperty.getUnauthorizedUrl());
        shiroFilterFactoryBean.setLoginUrl(shiroProperty.getLoginUrl());

        return shiroFilterFactoryBean;
    }


    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }

    @Bean(name = "exceptionHandler")
    public HandlerExceptionResolver handlerExceptionResolver() {
        return new GlobalExceptionResolver();
    }

}