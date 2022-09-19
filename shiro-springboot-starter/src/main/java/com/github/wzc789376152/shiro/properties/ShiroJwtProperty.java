package com.github.wzc789376152.shiro.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.shiro.jwt")
public class ShiroJwtProperty {
    private Boolean enable = false;
    private String header;
    private Long timeout;
    private Boolean multipleLogin = false;

    private List<String> ipWhileList;

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

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

    public Boolean getMultipleLogin() {
        return multipleLogin;
    }

    public void setMultipleLogin(Boolean multipleLogin) {
        this.multipleLogin = multipleLogin;
    }

    public List<String> getIpWhileList() {
        return ipWhileList;
    }

    public void setIpWhileList(List<String> ipWhileList) {
        this.ipWhileList = ipWhileList;
    }
}
