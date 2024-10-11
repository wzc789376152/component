package com.github.wzc789376152.springboot.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.wzc789376152.service.IResponseService;
import com.github.wzc789376152.service.impl.ResponseServiceImpl;
import com.github.wzc789376152.springboot.annotation.AppRestController;
import com.github.wzc789376152.springboot.annotation.OpenRestController;
import com.github.wzc789376152.springboot.utils.MDCUtils;
import com.github.wzc789376152.utils.DateUtils;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value(value = "${spring.jackson.date-format:yyyy-MM-dd HH:mm:ss}")
    private String dateFormat;

    /**
     * 使用此方法 解决, 以下 spring-boot: jackson时间格式化 配置 失效 问题
     * <p>
     * spring.jackson.time-zone=GMT+8
     * spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
     * 原因: 会覆盖 @EnableAutoConfiguration 关于 WebMvcAutoConfiguration 的配置
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //字符串转换器
        List<MediaType> listString = new ArrayList<>();
        //字符串的消息类型为text/plain
        listString.add(MediaType.TEXT_PLAIN);
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
        stringHttpMessageConverter.setSupportedMediaTypes(listString);
        // 生成JSON时,将所有Long转换成String
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = converter.getObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        simpleModule.addSerializer(BigDecimal.class, BigDecimalStringSerilizer.instance);
        simpleModule.addKeySerializer(BigDecimal.class, BigDecimalStringSerilizer.instance);
        objectMapper.registerModule(simpleModule);
        // 时间格式化
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //自动加载机制没有加载到 spring.jackson.date-format 配置，这里手动加进来
        objectMapper.setDateFormat(new SimpleDateFormat(dateFormat));
        // 设置格式化内容
        converter.setObjectMapper(objectMapper);
        //json转换器
        List<MediaType> list = new ArrayList<>();
        list.add(MediaType.APPLICATION_JSON);
        converter.setSupportedMediaTypes(list);
        converters.add(0, converter);
        converters.add(stringHttpMessageConverter);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/app", cls -> cls.isAnnotationPresent(AppRestController.class));
        configurer.addPathPrefix("/v", cls -> cls.isAnnotationPresent(OpenRestController.class));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestInterceptor());
    }

    public static class RequestInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String traceId = request.getHeader("traceId");
            if (StringUtils.isEmpty(traceId)) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            MDCUtils.put("traceId", traceId);
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            MDCUtils.remove("traceId");
        }
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new DateConvert());
    }

    @Bean
    @ConditionalOnMissingBean
    public IResponseService getResponseService() {
        return new ResponseServiceImpl();
    }

    public static class DateConvert implements Converter<String, Date> {
        @Override
        public Date convert(String s) {
            if (StringUtil.isNullOrEmpty(s)) {
                return null;
            }
            Date date = DateUtils.parseString(s);
            if (date == null) {
                throw new IllegalArgumentException("时间格式错误," + s);
            }
            return date;
        }
    }

    public static class BigDecimalStringSerilizer extends StdSerializer<BigDecimal> {
        public final static BigDecimalStringSerilizer instance = new BigDecimalStringSerilizer();

        public BigDecimalStringSerilizer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal bigDecimal, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if (bigDecimal == null) {
                jsonGenerator.writeString("0");
            } else {
                String val = bigDecimal.stripTrailingZeros().toPlainString();
                jsonGenerator.writeString(val);
            }
        }
    }
}
