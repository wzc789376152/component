package com.github.wzc789376152.springboot.config.shardingsphere;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Configuration
@ConfigurationProperties(prefix = "wzc.shardingsphere")
@Data
public class ShardingPropertics {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date minDate;
    private Boolean initTable = false;
}
