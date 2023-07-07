package com.github.wzc789376152.exception;

import lombok.Getter;

public class SystemException extends Exception{
    public static final Integer SYSTEM_ERROR_CODE = 500;
    public static final String SYSTEM_ERROR_MSG  = "系统异常，请稍后重试";

    @Getter
    private String errorCode;


    public SystemException() {
    }

    public SystemException(String errorMsg) {
        super(errorMsg);
    }

    public SystemException(Throwable e) {
        super(e);
    }

    public SystemException(String errorMsg, Throwable e) {
        super(errorMsg, e);
    }
    public SystemException(String errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;

    }
}
