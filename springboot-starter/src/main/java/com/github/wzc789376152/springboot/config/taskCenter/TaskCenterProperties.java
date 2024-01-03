package com.github.wzc789376152.springboot.config.taskCenter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wzc.task-center")
@Data
public class TaskCenterProperties {
    private Boolean enable = false;
    private Boolean initTable = false;
    private Boolean scheduled = false;
}
