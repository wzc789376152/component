package com.github.wzc789376152.shiro.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.shiro")
public class ShiroProperty {
    /**
     * 权限路由
     */
    private List<ShiroUrlPer> urlPers;
    /**
     * 成功回调地址
     */
    private String successUrl;
    /**
     * 默认登录地址
     */
    private String loginUrl;
    /**
     * 无权限访问地址
     */
    private String unauthorizedUrl;
    /**
     * 密码加密次数
     */
    private Integer hashIterations;
    /**
     * rememberme cookie加密的密钥 默认AES算法 密钥长度（128 256 512 位）
     */
    private String cipherKey;

    /**
     * session过期时间；-1L表示永不过期
     */
    private Long sessionTimeOut = 3600000L;

    /**
     * cookie过期时间：-1表示关闭浏览器过期
     */
    private Integer maxAge = -1;

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getUnauthorizedUrl() {
        return unauthorizedUrl;
    }

    public void setUnauthorizedUrl(String unauthorizedUrl) {
        this.unauthorizedUrl = unauthorizedUrl;
    }


    public Integer getHashIterations() {
        return hashIterations;
    }

    public void setHashIterations(Integer hashIterations) {
        this.hashIterations = hashIterations;
    }

    public List<ShiroUrlPer> getUrlPers() {
        return urlPers;
    }

    public void setUrlPers(List<ShiroUrlPer> urlPers) {
        this.urlPers = urlPers;
    }

    public String getCipherKey() {
        return cipherKey;
    }

    public void setCipherKey(String cipherKey) {
        this.cipherKey = cipherKey;
    }

    public Long getSessionTimeOut() {
        return sessionTimeOut;
    }

    public void setSessionTimeOut(Long sessionTimeOut) {
        this.sessionTimeOut = sessionTimeOut;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }
}
