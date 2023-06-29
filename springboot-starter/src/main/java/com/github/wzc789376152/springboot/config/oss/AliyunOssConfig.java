package com.github.wzc789376152.springboot.config.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wzc.oss")
@ConditionalOnProperty(prefix = "wzc.oss", name = "enable",havingValue = "true")
@Data
public class AliyunOssConfig {
    /**
     * 地域节点
     */
    private Boolean enable = false;
    private String endPoint;
    private String accessKeyId;
    private String accessKeySecret;
    /**
     * bucketName
     */
    private String bucketName;
    /**
     * 域名
     */
    private String domain;
    /**
     * 子文件夹
     */
    private String prefix;

    @Bean
    public OSS ossClient() {
        if (!this.enable) {
            return null;
        }
        return new OSSClientBuilder().build(endPoint, accessKeyId, accessKeySecret);
    }

    @Bean
    public AliyunOssService aliyunOssService() {
        if (!this.enable) {
            return null;
        }
        return new AliyunOssService(ossClient(), this);
    }
}
