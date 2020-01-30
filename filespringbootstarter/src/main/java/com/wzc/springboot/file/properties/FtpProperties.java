package com.wzc.springboot.file.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cqfile.ftp")
public class FtpProperties extends com.wzc.common.file.config.ftp.FtpProperties {
}
