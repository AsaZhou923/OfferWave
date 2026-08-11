package com.offerwave.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces a single pagination boundary for every API controller.
 */
public class PaginationParameterInterceptor implements HandlerInterceptor {

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        validatePositiveInteger(request.getParameter("page"), "page", Integer.MAX_VALUE);
        validatePositiveInteger(request.getParameter("size"), "size", MAX_PAGE_SIZE);
        return true;
    }

    private void validatePositiveInteger(String rawValue, String parameterName, int maximum) {
        if (rawValue == null) {
            return;
        }

        final int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(parameterName + " 必须是整数");
        }

        if (value < 1 || value > maximum) {
            if ("size".equals(parameterName)) {
                throw new IllegalArgumentException("size 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
            }
            throw new IllegalArgumentException("page 必须大于或等于 1");
        }
    }
}
