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
     * 密码加密次数
     */
    private Integer hashIterations;


    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
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
}
