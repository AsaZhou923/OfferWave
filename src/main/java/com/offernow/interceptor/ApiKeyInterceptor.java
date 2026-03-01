package com.offernow.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部接口 API Key 拦截器（历史兼容组件）。
 *
 * 当前默认鉴权策略已切换为管理员 JWT，该拦截器暂未在 WebMvcConfig 中注册。
 * 若后续需要恢复 API Key 方案，可在 WebMvcConfig 中按需启用。
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    /** 后端配置中的爬虫调用密钥 */
    @Value("${offernow.crawler.api-key}")
    private String configuredApiKey;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {
        if (!StringUtils.hasText(configuredApiKey)) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"code\": 503, \"message\": \"Crawler API Key is not configured\"}");
            return false;
        }

        String apiKey = request.getHeader("X-API-KEY");
        if (!StringUtils.hasText(apiKey) || !apiKey.equals(configuredApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\": 401, \"message\": \"Invalid or missing API Key\"}");
            return false;
        }

        return true;
    }
}
