package com.offernow.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(PrivilegeException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // 返回 HTTP 403
    public R<Object> handlePrivilegeException(PrivilegeException ex) {
        // 根据 API 文档，失败时需要返回引导升级的 data
        Map<String, Object> data = new HashMap<>();
        data.put("upgrade_url", "/memberships");
        R<Object> r = R.error(403, ex.getMessage());
        return r.add("data", data);
    }

    /**
     * 处理其他通用异常 (可选，作为兜底)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 返回 HTTP 500
    public R<String> handleException(Exception ex) {
        ex.printStackTrace(); // 在生产环境中应使用日志记录
        return R.error("服务器内部错误: " + ex.getMessage());
    }
}
