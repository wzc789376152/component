package com.github.wzc789376152.file.config;

import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.manager.ftp.FtpFileManagerAbstract;
import com.github.wzc789376152.file.properties.FtpProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Order(1)
@Component
@ConditionalOnProperty(prefix = "spring.cqfile.ftp", name = "enable", havingValue = "true")
@EnableConfigurationProperties(FtpProperties.class)
public class FtpFileManager extends FtpFileManagerAbstract {
    @Resource
    private FtpProperties properties;

    @Override
    public com.github.wzc789376152.file.config.ftp.FtpProperties ftpProperties() {
        return properties;
    }
}
