package com.github.wzc789376152.utils;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

public class JSONUtils {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        simpleModule.addSerializer(BigDecimal.class, BigDecimalStringSerilizer.instance);
        simpleModule.addKeySerializer(BigDecimal.class, BigDecimalStringSerilizer.instance);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        objectMapper.registerModule(simpleModule);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
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

    public static String toJSONString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parse(Object value, Class<T> tClass) {
        if (value == null) {
            return null;
        }
        if (value.getClass().getName().equals(tClass.getName())) {
            return (T) value;
        }
        if (value instanceof String) {
            return JSON.parseObject(value.toString(), tClass);
        }
        if (value instanceof byte[]) {
            return JSON.parseObject(new String((byte[]) value, StandardCharsets.UTF_8), tClass);
        }
        return JSON.parseObject(toJSONString(value), tClass);
    }

    public static <T> List<T> parseArray(Object value, Class<T> tClass) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return JSON.parseArray(value.toString(), tClass);
        }
        if (value instanceof byte[]) {
            return JSON.parseArray(new String((byte[]) value, StandardCharsets.UTF_8), tClass);
        }
        return JSON.parseArray(toJSONString(value), tClass);
    }
}
