package com.wzc.springboot.file.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cqfile.local")
public class LocalProperties extends com.wzc.common.file.config.local.LocalProperties {
}
