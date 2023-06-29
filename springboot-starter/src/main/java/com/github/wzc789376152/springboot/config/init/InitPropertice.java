package com.github.wzc789376152.springboot.config.init;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class InitPropertice {
    @Value("${spring.application.name}")
    private String serverName;

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.profiles.active}")
    private String profilesActive;
}
