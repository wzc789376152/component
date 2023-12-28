package com.github.wzc789376152.vo;


import lombok.Data;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.UUID;

@Data
public class RetResult<T> implements Serializable, Cloneable {

    // 请求流水号
    private String requestId;

    public String getRequestId() {
        if (StringUtils.isNotEmpty(requestId)) {
            return requestId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    public Integer code;

    private String message;

    private T data;

    public RetResult(String requestId, Integer code, String msg, T data) {
        this.code = code;
        this.message = msg;
        this.data = data;
        this.requestId = requestId;
    }


    public static <T> RetResult<T> success() {
        return success(null, null, null);
    }

    public static <T> RetResult<T> success(String uuid, T data) {

        return success(uuid, data, null);
    }

    public static <T> RetResult<T> success(T data) {
        return success(data, null);
    }

    public static <T> RetResult<T> success(T data, String message) {
        return success(null, data, message);
    }

    public static <T> RetResult<T> success(String uuid, T data, String message) {
        return new RetResult<>(uuid, 200, message, data);
    }

    public static <T> RetResult<T> failed(String msg) {
        return failed(null, 500, msg, null);
    }

    public static <T> RetResult<T> failed(String uuid, T data) {
        return failed(uuid, 500, null, data);
    }

    public static <T> RetResult<T> failed(Integer code, String msg, T data) {
        return failed(null, code, msg, data);
    }

    public static <T> RetResult<T> failed(String uuid, Integer code, String msg, T data) {
        return new RetResult<>(uuid, code, msg, data);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public Object clone() {
        return SerializationUtils.clone(this);
    }

}
