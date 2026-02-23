package com.offernow.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部接口 API Key 拦截器。
 * 仅用于 /api/v1/internal/** 路径的访问控制。
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    /** 后端配置中的爬虫调用密钥 */
    @Value("${offernow.crawler.api-key}")
    private String configuredApiKey;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {
        String apiKey = request.getHeader("X-API-KEY");

        if (apiKey == null || !apiKey.equals(configuredApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\": 401, \"message\": \"Invalid or missing API Key\"}");
            return false;
        }
        return true;
    }
}
