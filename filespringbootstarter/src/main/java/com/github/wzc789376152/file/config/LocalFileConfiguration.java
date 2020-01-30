package com.github.wzc789376152.file.config;


import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.manager.local.LocalFileMananger;
import com.github.wzc789376152.file.properties.LocalProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(LocalProperties.class)
@ConditionalOnProperty(prefix = "spring.cqfile", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileConfiguration {
    @Resource
    LocalProperties localProperties;

    @Bean
    @ConditionalOnMissingBean(IFileManager.class)
    public IFileManager fileManager() {
        return new LocalFileMananger(localProperties);
    }
}