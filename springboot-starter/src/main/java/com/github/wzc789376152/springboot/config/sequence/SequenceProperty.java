package com.github.wzc789376152.springboot.config.sequence;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wzc.sequence")
@ConditionalOnProperty(prefix = "wzc.sequence", name = "enable", havingValue = "true", matchIfMissing = true)
@Data
public class SequenceProperty {
    private Boolean enable = false;
    /**
     * 生成类型REDIS,ASSIGN_ID
     */
    private SequenceType type;
    private Integer length = 4;
}
