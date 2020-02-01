package com.github.wzc789376152.file.config;

import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.manager.ftp.FtpFileManagerAbstract;
import com.github.wzc789376152.file.properties.FtpProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(FtpProperties.class)
@ConditionalOnProperty(prefix = "spring.cqfile.ftp", name = "enable", havingValue = "true", matchIfMissing = true)
public class FtpFileConfiguration {
    @Resource
    private FtpProperties properties;

    @Bean
    @ConditionalOnMissingBean(IFileManager.class)
    public IFileManager fileManager() {
        return new FtpFileManagerAbstract() {
            @Override
            public com.github.wzc789376152.file.config.ftp.FtpProperties ftpProperties() {
                return properties;
            }
        };
    }

    public FtpProperties getProperties() {
        return properties;
    }

    public void setProperties(FtpProperties properties) {
        this.properties = properties;
    }
}
