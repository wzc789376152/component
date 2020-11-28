package com.github.wzc789376152.shiro.config;

import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.properties.ShiroProperty;
import com.github.wzc789376152.shiro.properties.ShiroSessionProperty;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.shiro.session", name = "enable", havingValue = "true")
@EnableConfigurationProperties(ShiroSessionProperty.class)
public class ShiroSessionConfiguration {
    @Autowired
    private ShiroSessionProperty shiroSessionProperty;
    @Autowired(required = false)
    private SessionDAO sessionDAO;

    /**
     * cookie管理器;
     *
     * @return
     */
    @Bean
    public CookieRememberMeManager rememberMeManager() {
        CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
        //rememberme cookie加密的密钥 建议每个项目都不一样 默认AES算法 密钥长度（128 256 512 位），通过以下代码可以获取
        byte[] cipherKey = Base64.decode(shiroSessionProperty.getCipherKey());
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
        simpleCookie.setMaxAge(shiroSessionProperty.getMaxAge());
        return simpleCookie;
    }

    @Bean(name = "sessionManager")
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setGlobalSessionTimeout(shiroSessionProperty.getSessionTimeOut());
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
        simpleCookie.setMaxAge(shiroSessionProperty.getMaxAge());
        return simpleCookie;
    }
}
