package com.github.wzc789376152.springboot.config;


import com.github.wzc789376152.exception.BizRuntimeException;
import com.github.wzc789376152.exception.SystemException;
import com.github.wzc789376152.springboot.annotation.NoResultFormatter;
import com.github.wzc789376152.utils.JSONUtils;
import com.github.wzc789376152.vo.RetResult;
import feign.FeignException;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

@Slf4j
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

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = DuplicateKeyException.class)
    public RetResult<Object> exception(DuplicateKeyException e) {
        log.error("唯一索引异常", e);
        return RetResult.failed(500, "记录已存在，无法重复添加", null);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public RetResult<Object> exception(MissingServletRequestParameterException e) {
        String format = MessageFormat.format("参数 {0} 类型 {1} 解析失败", e.getParameterName(), e.getParameterType());
        log.error(format, e);
        return RetResult.failed(400, format, null);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = BindException.class)
    public RetResult<Object> exception(BindException exception, HttpServletResponse response) {
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
        response.setStatus(400);
        return RetResult.failed(400, builder.toString(), null);
    }

    /**
     * 处理请求单个参数不满足校验规则的异常信息
     *
     * @param exception 异常
     * @return RetResult
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = ConstraintViolationException.class)
    public RetResult<Object> exception(ConstraintViolationException exception, HttpServletResponse response) {
        log.error(exception.getMessage(), exception);
        response.setStatus(400);
        return RetResult.failed(400, exception.getMessage(), null);
    }

    /**
     * 处理自定义异常
     *
     * @param exception 异常
     * @return RetResult
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = BizRuntimeException.class)
    public RetResult<Object> exception(BizRuntimeException exception, HttpServletResponse response) {
        log.error(exception.getMessage(), exception);
        response.setStatus(500);
        return RetResult.failed(ObjectUtils.isNotEmpty(exception.getErrorCode()) ? exception.getErrorCode() : 500, exception.getMessage(), null);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public RetResult<Object> exception(HttpMessageNotReadableException exception, HttpServletResponse response) {
        log.error(exception.getMessage(), exception);
        response.setStatus(400);
        return RetResult.failed(400, "请输入正确数据格式", null);
    }

    @ExceptionHandler({FeignException.class})
    public RetResult<Object> exception(FeignException exception, HttpServletResponse response) {
        response.setStatus(exception.status());
        String message = exception.getMessage();
        if (message.contains("[{")) {
            message = message.substring(message.lastIndexOf("[{") + 1, message.lastIndexOf("}]") + 1);
            RetResult result = JSONUtils.parse(message, RetResult.class);
            return result;
        } else if (message.contains("\"]")) {
            message = message.substring(message.lastIndexOf("[\"") + 2, message.lastIndexOf("\"]"));
        }
        return RetResult.failed(exception.status(), message, (Object) null);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {SystemException.class, Exception.class})
    public RetResult<Object> exception(Exception exception) {
        log.error(exception.getMessage(), exception);
        if (exception.getCause() != null && exception.getCause() instanceof BizRuntimeException) {
            return exception((BizRuntimeException) exception.getCause());
        }
        return RetResult.failed(500, SystemException.SYSTEM_ERROR_MSG, null);
    }
}
