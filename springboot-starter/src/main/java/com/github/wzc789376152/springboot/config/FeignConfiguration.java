package com.github.wzc789376152.springboot.config;

import cn.hutool.core.net.URLEncoder;
import com.github.wzc789376152.utils.TokenUtils;
import com.github.wzc789376152.vo.UserInfo;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableFeignClients(basePackages = {"**.feign"})
public class FeignConfiguration implements RequestInterceptor {
    URLEncoder urlEncoder = new URLEncoder();

    @Override
    public void apply(RequestTemplate requestTemplate) {
        setHeader(requestTemplate, "FeignResultFormat", true);
        UserInfo userInfo = TokenUtils.getCurrentUser();
        if (userInfo != null) {
            setToken(requestTemplate, userInfo.getToken());
        }
    }

    private void setHeader(RequestTemplate requestTemplate, String key, Object value) {
        if (value != null) {
            requestTemplate.header(key, urlEncoder.encode(value.toString(), StandardCharsets.UTF_8));
        }
    }

    private void setToken(RequestTemplate requestTemplate, String token) {
        requestTemplate.header("token", token);
    }

}
