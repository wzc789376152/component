package com.github.wzc789376152.shiro.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.shiro.session")
public class ShiroSessionProperty {
    private Boolean enable = false;
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

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
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
