package com.offernow.common;

import org.springframework.http.HttpStatus;
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
     * 处理兜底异常。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<String> handleException(Exception ex) {
        ex.printStackTrace();
        return R.error("服务器内部错误: " + ex.getMessage());
    }
}
