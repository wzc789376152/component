package com.github.wzc789376152.file.config;

import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.service.IFileService;
import com.github.wzc789376152.file.service.impl.FileServiceImpl;
import com.github.wzc789376152.file.properties.FileProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileProperties.class)
@ConditionalOnClass(IFileService.class)
public class FileConfiguration implements IFileConfiguration {
    @Autowired
    FileProperties fileProperties;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    IFileManager fileManager;

    @Bean
    @ConditionalOnMissingBean(IFileService.class)
    public IFileService fileService() {
        return new FileServiceImpl() {
            @Override
            public FileProperties getProperties() {
                return fileProperties;
            }

            @Override
            public IFileManager getFileManager() {
                return fileManager();
            }
        };
    }

    public IFileManager fileManager() {
        return fileManager;
    }
}
