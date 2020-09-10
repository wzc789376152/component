package com.github.wzc789376152.file.config;

import com.github.wzc789376152.file.manager.SmbFileManagerAbstract;
import com.github.wzc789376152.file.properties.SmbProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Order(-1)
@Primary
@Component
@ConditionalOnProperty(prefix = "spring.cqfile.smb", name = "enable", havingValue = "true")
@EnableConfigurationProperties(SmbProperties.class)
public class SmbFileManager extends SmbFileManagerAbstract {
    @Resource
    private SmbProperties properties;
    @Override
    public com.github.wzc789376152.file.SmbProperties getSmbProperties() {
        return properties;
    }
}
