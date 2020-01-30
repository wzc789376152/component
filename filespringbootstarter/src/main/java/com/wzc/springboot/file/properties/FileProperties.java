package com.wzc.springboot.file.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cqfile")
public class FileProperties extends com.wzc.common.file.FileProperties {
}