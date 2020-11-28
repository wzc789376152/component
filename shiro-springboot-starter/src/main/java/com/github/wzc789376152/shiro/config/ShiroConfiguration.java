package com.github.wzc789376152.shiro.config;

import com.github.wzc789376152.shiro.exception.GlobalExceptionResolver;
import com.github.wzc789376152.shiro.filter.JwtFilter;
import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.properties.ShiroProperty;
import com.github.wzc789376152.shiro.properties.ShiroUrlPer;
import com.github.wzc789376152.shiro.realm.ShiroJwtRealm;
import com.github.wzc789376152.shiro.realm.ShiroPasswordRealm;
import com.github.wzc789376152.shiro.realm.UserModularRealmAuthenticator;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.shiro.mgt.SecurityManager;
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
@EnableConfigurationProperties(value = {ShiroProperty.class})
public class ShiroConfiguration {
    @Autowired
    private ShiroProperty shiroProperty;
    @Autowired(required = false)
    private ShiroJwtProperty shiroJwtProperty;
    @Resource(name = "shiroRedisCacheManager")
    private CacheManager cacheManager;
    @Autowired(required = false)
    private SessionDAO sessionDAO;
    @Autowired(required = false)
    private ShiroJwtRealm shiroJwtRealm;

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
        realms.add(shiroRealm());
        if (shiroJwtRealm != null) {
            securityManager.setAuthenticator(userModularRealmAuthenticator);
            realms.add(shiroJwtRealm);
        }
        securityManager.setRealms(realms);
        if (shiroProperty.getEnableSession()) {
            if (cacheManager == null) {
                cacheManager = ehCacheManager();
            }
            securityManager.setCacheManager(cacheManager);
            securityManager.setRememberMeManager(rememberMeManager());
            securityManager.setSessionManager(sessionManager());
        } else {
            DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
            DefaultSessionStorageEvaluator defaultSessionStorageEvaluator = new DefaultSessionStorageEvaluator();
            defaultSessionStorageEvaluator.setSessionStorageEnabled(false);
            subjectDAO.setSessionStorageEvaluator(defaultSessionStorageEvaluator);
            securityManager.setSubjectDAO(subjectDAO);
        }
        return securityManager;
    }

    /**
     * cookie管理器;
     *
     * @return
     */
    @Bean
    public CookieRememberMeManager rememberMeManager() {
        CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
        //rememberme cookie加密的密钥 建议每个项目都不一样 默认AES算法 密钥长度（128 256 512 位），通过以下代码可以获取
        byte[] cipherKey = Base64.decode(shiroProperty.getCipherKey());
        cookieRememberMeManager.setCipherKey(cipherKey);
        cookieRememberMeManager.setCookie(rememberMeCookie());
        return cookieRememberMeManager;
    }

    @Bean
    public SimpleCookie rememberMeCookie() {
        //这个参数是cookie的名称，对应前端的checkbox的name = rememberMe
        SimpleCookie simpleCookie = new SimpleCookie("rememberMe");
        //如果httyOnly设置为true，则客户端不会暴露给客户端脚本代码，使用HttpOnly cookie有助于减少某些类型的跨站点脚本攻击；
        simpleCookie.setHttpOnly(true);
        //记住我cookie生效时间
        simpleCookie.setMaxAge(shiroProperty.getMaxAge());
        return simpleCookie;
    }

    @Bean(name = "sessionManager")
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setGlobalSessionTimeout(shiroProperty.getSessionTimeOut());
        if (sessionDAO != null) {
            sessionManager.setSessionDAO(sessionDAO);
        }
        sessionManager.setSessionIdCookie(sessionIdCookie());
        return sessionManager;
    }

    @Bean("sessionIdCookie")
    public SimpleCookie sessionIdCookie() {
        //这个参数是cookie的名称
        SimpleCookie simpleCookie = new SimpleCookie("JSESSIONID");
        //setcookie的httponly属性如果设为true的话，会增加对xss防护的安全系数。它有以下特点：
        //setcookie()的第七个参数
        //设为true后，只能通过http访问，javascript无法访问
        //防止xss读取cookie
        simpleCookie.setHttpOnly(true);
        simpleCookie.setPath("/");
        //maxAge=-1表示浏览器关闭时失效此Cookie
        simpleCookie.setMaxAge(shiroProperty.getMaxAge());
        return simpleCookie;
    }

    @Bean(name = "shiroFilter")
    public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        Map<String, String> filterChainDefinitionManager = new LinkedHashMap<String, String>();
        if (shiroProperty.getUrlPers() != null && shiroProperty.getUrlPers().size() > 0) {
            for (int i = 0; i < shiroProperty.getUrlPers().size(); i++) {
                ShiroUrlPer shiroUrlPer = shiroProperty.getUrlPers().get(i);
                filterChainDefinitionManager.put(shiroUrlPer.getUrl(), shiroUrlPer.getPer());
            }
        }
        Map<String, Filter> filter = new LinkedHashMap<>(1);
        if (shiroJwtProperty != null && shiroJwtProperty.getEnable()) {
            filter.put("jwt", new JwtFilter(shiroJwtProperty));
            filterChainDefinitionManager.put("/**", "jwt");
        }
        shiroFilterFactoryBean.setFilters(filter);
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionManager);
        shiroFilterFactoryBean.setSuccessUrl(shiroProperty.getSuccessUrl());
        shiroFilterFactoryBean.setUnauthorizedUrl(shiroProperty.getUnauthorizedUrl());
        shiroFilterFactoryBean.setLoginUrl(shiroProperty.getLoginUrl());

        return shiroFilterFactoryBean;
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator proxyCreator = new DefaultAdvisorAutoProxyCreator();
        proxyCreator.setProxyTargetClass(true);
        return proxyCreator;
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

    public EhCacheManager ehCacheManager() {
        EhCacheManager ehCacheManager = new EhCacheManager();
        return ehCacheManager;
    }
}