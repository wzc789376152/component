package com.github.wzc789376152.springboot.config;

import cn.hutool.core.net.URLEncoder;
import com.github.wzc789376152.utils.TokenUtils;
import com.github.wzc789376152.vo.UserInfo;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Configuration
@EnableFeignClients(basePackages = {"**.feign"})
public class FeignConfiguration implements RequestInterceptor {
    URLEncoder urlEncoder = new URLEncoder();

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        //防止空指针
        if (servletRequestAttributes != null) {
            //获取原Request对象
            HttpServletRequest request = servletRequestAttributes.getRequest();
            //把原request的请求头的所有参数都拿出来
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                //获取每个请求头参数的名字
                String name = headerNames.nextElement();
                //获取值
                String value = request.getHeader(name);
                if ("content-length".equalsIgnoreCase(name)) {
                    continue;
                }
                //放到feign调用对象的request中去
                requestTemplate.header(name, value);
            }
        }
        setHeader(requestTemplate, "FeignResultFormat", true);
        setHeader(requestTemplate, "traceId", MDC.get("traceId"));
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
