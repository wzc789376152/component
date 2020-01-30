package com.github.wzc789376152.file.config;

import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.manager.ftp.FtpFileManager;
import com.github.wzc789376152.file.properties.FtpProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(FtpProperties.class)
@ConditionalOnProperty(prefix = "spring.cqfile", name = "type", havingValue = "ftp", matchIfMissing = true)
public class FtpFileConfiguration {
    @Resource
    private FtpProperties ftpProperties;

    @Bean
    @ConditionalOnMissingBean(IFileManager.class)
    public IFileManager fileManager() {
        return new FtpFileManager(ftpProperties);
    }
}