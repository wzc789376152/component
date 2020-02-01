package com.github.wzc789376152.file.config;

import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.manager.local.LocalFileManangerAbstract;
import com.github.wzc789376152.file.properties.FileProperties;
import com.github.wzc789376152.file.service.IFileService;
import com.github.wzc789376152.file.service.impl.FileServiceAbstract;
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
    FileProperties properties;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    IFileManager manager;

    @Bean
    @ConditionalOnMissingBean(IFileService.class)
    public IFileService fileService() {
        return new FileServiceAbstract() {
            @Override
            public FileProperties getProperties() {
                return properties;
            }

            @Override
            public IFileManager getFileManager() {
                if (manager == null) {
                    manager = new LocalFileManangerAbstract();
                }
                return manager;
            }
        };
    }
    public IFileManager getFileManager() {
        return manager;
    }
}
