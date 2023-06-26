package com.github.wzc789376152.shiro.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.shiro.jwt")
public class ShiroJwtProperty {
    private Boolean enable = false;
    private List<String> headers;
    private Long timeout;
    private String secret;

    private List<String> ipWhileList;


    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public List<String> getIpWhileList() {
        return ipWhileList;
    }

    public void setIpWhileList(List<String> ipWhileList) {
        this.ipWhileList = ipWhileList;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }
}
