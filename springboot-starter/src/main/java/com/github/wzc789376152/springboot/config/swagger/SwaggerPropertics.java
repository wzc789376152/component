package com.github.wzc789376152.springboot.config.swagger;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wzc.swagger")
public class SwaggerPropertics {
    private Boolean enable = false;
    /**
     * 标题
     */
    private String title;
    /**
     * 接口包名
     */
    private String basePackage;
    /**
     * 对外接口包名
     */
    private String baseApiPackage;
    /**
     * 描述
     */
    private String description;
    /**
     * 版本号
     */
    private String version;

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getBaseApiPackage() {
        return baseApiPackage;
    }

    public void setBaseApiPackage(String baseApiPackage) {
        this.baseApiPackage = baseApiPackage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
