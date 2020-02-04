package com.github.wzc789376152.file.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cqfile.ftp")
public class FtpProperties extends com.github.wzc789376152.file.FtpProperties {
    /**
     * 是否启用ftp管理器
     */
    private boolean enable = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
