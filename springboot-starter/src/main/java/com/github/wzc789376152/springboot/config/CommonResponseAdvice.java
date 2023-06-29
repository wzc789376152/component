package com.github.wzc789376152.springboot.config;


import com.github.wzc789376152.springboot.annotation.NoResultFormatter;
import com.github.wzc789376152.utils.JSONUtils;
import com.github.wzc789376152.vo.RetResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Objects;

public abstract class CommonResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return true;
    }


    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        NoResultFormatter noResultFormatter = AnnotationUtils.findAnnotation(Objects.requireNonNull(methodParameter.getMethod()), NoResultFormatter.class);
        if (noResultFormatter == null) {
            if (!(o instanceof RetResult)) {
                RetResult<Object> retResult = RetResult.success(o);
                ApiOperation apiOperation = AnnotationUtils.findAnnotation(Objects.requireNonNull(methodParameter.getMethod()), ApiOperation.class);
                if (apiOperation != null) {
                    retResult.setMessage(apiOperation.value() + "成功");
                }
                o = retResult;
            }
        }

        if (mediaType.equals(MediaType.APPLICATION_JSON)) {
            return o;
        }
        return JSONUtils.toJSONString(o);
    }
}
