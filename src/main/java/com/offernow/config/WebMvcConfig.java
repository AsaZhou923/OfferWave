package com.offernow.config;

import com.offernow.interceptor.ApiKeyInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置：用于注册请求拦截器。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ApiKeyInterceptor apiKeyInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 仅对内部接口启用 API Key 认证拦截器
        // 用户登录态认证已由 Spring Security + JwtAuthenticationFilter 处理
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/v1/internal/**");
    }
}
