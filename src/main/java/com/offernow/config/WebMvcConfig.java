package com.offernow.config;

import com.offernow.interceptor.ApiKeyInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置，用于注册拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

        @Autowired
        private ApiKeyInterceptor apiKeyInterceptor;

        @Override
        public void addInterceptors(@NonNull InterceptorRegistry registry) {
                // 仅保留用于内部 API 的 Key 认证拦截器
                // 原有的 AuthInterceptor 已被 Spring Security 的 JwtAuthenticationFilter 取代
                registry.addInterceptor(apiKeyInterceptor)
                                .addPathPatterns("/api/v1/internal/**");
        }
}
