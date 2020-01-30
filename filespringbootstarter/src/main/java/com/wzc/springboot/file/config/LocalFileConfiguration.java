package com.wzc.springboot.file.config;


import com.wzc.common.file.manager.IFileManager;
import com.wzc.common.file.manager.local.LocalFileMananger;
import com.wzc.springboot.file.properties.LocalProperties;
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