package com.github.wzc789376152.springboot.config;

import cn.hutool.core.net.URLEncoder;
import com.github.wzc789376152.springboot.utils.MDCUtils;
import com.github.wzc789376152.utils.TokenUtils;
import com.github.wzc789376152.vo.UserInfo;
import org.slf4j.MDC;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@Configuration
@EnableDiscoveryClient
public class RestTemplateConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 设置通用的请求头
        HeaderRequestInterceptor tokenHeader = new HeaderRequestInterceptor();
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(tokenHeader);
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    /**
     * 拦截器类，为restTemplatep后续调用请求时携带请求头
     */
    public static class HeaderRequestInterceptor implements ClientHttpRequestInterceptor {
        URLEncoder urlEncoder = new URLEncoder();

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            UserInfo userInfo = TokenUtils.getCurrentUser();
            if (userInfo != null) {
                setToken(request, userInfo.getToken());
            }
            request.getHeaders().set("FeignResultFormat", "true");
            request.getHeaders().set("traceId", MDCUtils.get("traceId"));
            return execution.execute(request, body);
        }

        private void setHeader(HttpRequest request, String key, Object value) {
            if (value != null) {
                request.getHeaders().set(key, urlEncoder.encode(value.toString(), StandardCharsets.UTF_8));
            }
        }

        private void setToken(HttpRequest request, String token) {
            request.getHeaders().set("token", token);
        }

    }
}
