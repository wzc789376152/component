package com.github.wzc789376152.file.config;

import com.github.wzc789376152.file.manager.FtpFileManagerAbstract;
import com.github.wzc789376152.file.properties.FtpProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Order(1)
@Primary
@Component
@ConditionalOnProperty(prefix = "spring.cqfile.ftp", name = "enable", havingValue = "true")
@EnableConfigurationProperties(FtpProperties.class)
public class FtpFileManager extends FtpFileManagerAbstract {
    @Resource
    private FtpProperties properties;

    @Override
    public com.github.wzc789376152.file.FtpProperties ftpProperties() {
        return properties;
    }

    public String getDownloadUrl(String filename) {
        return null;
    }
}
