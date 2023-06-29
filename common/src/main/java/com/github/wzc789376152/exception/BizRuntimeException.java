package com.github.wzc789376152.exception;


import lombok.Getter;


public class BizRuntimeException extends  RuntimeException{

    @Getter
    private String errorCode;

    @Getter
    private String errorMiniCode;

    public BizRuntimeException() {
        super();
    }

    public BizRuntimeException(String message) {
        super(message);
    }

    public BizRuntimeException(String errorCode, String errorMiniCode,String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMiniCode = errorMiniCode;
    }

    public BizRuntimeException(String errorMsg, Throwable e) {
        super(errorMsg, e);
    }
}
