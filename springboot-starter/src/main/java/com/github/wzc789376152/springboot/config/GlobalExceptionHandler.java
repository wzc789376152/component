package com.github.wzc789376152.springboot.config;


import com.github.wzc789376152.exception.BizRuntimeException;
import com.github.wzc789376152.exception.SystemException;
import com.github.wzc789376152.vo.RetResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.text.MessageFormat;
import java.util.List;

/**
 * 定义全局异常捕获类
 * <p>
 * 统一的返回格式
 *
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = DuplicateKeyException.class)
    public RetResult<Object> exception(DuplicateKeyException e) {
        log.error("唯一索引异常", e);
        return RetResult.failed("500", "记录已存在，无法重复添加", null);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public RetResult<Object> exception(MissingServletRequestParameterException e) {
        String format = MessageFormat.format("参数 {0} 类型 {1} 解析失败", e.getParameterName(), e.getParameterType());
        log.error(format, e);
        return RetResult.failed("500", format, null);
    }

    @ExceptionHandler(value = BindException.class)
    public RetResult<Object> exception(BindException exception) {
        BindingResult result = exception.getBindingResult();
        final List<FieldError> fieldErrors = result.getFieldErrors();
        StringBuilder builder = new StringBuilder();
        for (FieldError error : fieldErrors) {
            String message = error.getDefaultMessage();
            if (message != null && message.contains("IllegalArgumentException")) {
                message = message.substring(message.lastIndexOf("IllegalArgumentException") + 1);
                message = message.substring(message.indexOf(":") + 1);
                message = message.substring(0, message.contains(";") ? message.indexOf(";") : message.length() - 1);
            }
            builder.append(message).append("\n");
        }
        log.error(builder.toString(), exception);
        return RetResult.failed("400", builder.toString(), null);
    }

    /**
     * 处理请求单个参数不满足校验规则的异常信息
     *
     * @param exception 异常
     * @return RetResult
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public RetResult<Object> exception(ConstraintViolationException exception) {
        log.error(exception.getMessage(), exception);
        return RetResult.failed("500", exception.getMessage(), null);
    }

    /**
     * 处理自定义异常
     * @param exception 异常
     * @return RetResult
     */

    @ExceptionHandler(value = BizRuntimeException.class)
    public RetResult<Object> exception(BizRuntimeException exception) {
        log.error(exception.getMessage(), exception);
        return RetResult.failed(StringUtils.isNotEmpty(exception.getErrorCode()) ? exception.getErrorCode() : "500", exception.getMessage(), null);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public RetResult<Object> exception(HttpMessageNotReadableException exception) {
        log.error(exception.getMessage(), exception);
        return RetResult.failed("400", "请输入正确数据格式", null);
    }

    @ExceptionHandler(value = {SystemException.class, Exception.class})
    public RetResult<Object> exception(Exception exception) {
        log.error(exception.getMessage(), exception);
        if (exception.getCause() != null && exception.getCause() instanceof BizRuntimeException) {
            return exception((BizRuntimeException) exception.getCause());
        }
        return RetResult.failed("500", SystemException.SYSTEM_ERROR_MSG, null);
    }
}