package com.offerwave.common;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理权限不足异常。
     */
    @ExceptionHandler(PrivilegeException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Object> handlePrivilegeException(PrivilegeException ex) {
        Map<String, Object> data = new HashMap<>();
        data.put("upgrade_url", "/memberships");
        R<Object> r = R.error(403, ex.getMessage());
        return r.add("data", data);
    }

    /**
     * 处理资源不存在异常。
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<String> handleNotFoundException(NotFoundException ex) {
        return R.error(404, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return R.error(400, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数不合法" : fieldError.getDefaultMessage();
        return R.error(400, message);
    }

    /**
     * 处理兜底异常。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<String> handleException(Exception ex) {
        ex.printStackTrace();
        return R.error("服务器内部错误: " + ex.getMessage());
    }
}
