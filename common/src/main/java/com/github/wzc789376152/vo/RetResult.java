package com.github.wzc789376152.vo;


import lombok.Data;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.UUID;

@Data
public class RetResult<T> implements Serializable, Cloneable {

    // 请求流水号
    private String requestId = UUID.randomUUID().toString().replace("-", "");

    public Integer code;

    private String message;

    private T data;

    public RetResult() {
    }

    public RetResult(Integer code, String msg, T data) {
        this.code = code;
        this.message = msg;
        this.data = data;
    }

    public RetResult(Integer code, T data) {
        this.data = data;
        this.code = code;
    }

    public static <T> RetResult<T> success() {
        return new RetResult<>(200, null);
    }


    public static <T> RetResult<T> success(T data) {
        RetResult<T> result = RetResult.success();
        result.setData(data);
        return result;
    }

    public static <T> RetResult<T> success(T data, String message) {
        RetResult<T> result = RetResult.success();
        result.setData(data);
        result.setMessage(message);
        return result;
    }

    public static <T> RetResult<T> failed(T data) {
        return new RetResult<>(500, data);
    }

    public static <T> RetResult<T> failed(Integer code, String msg, T data) {
        return new RetResult<>(code, msg, data);
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
