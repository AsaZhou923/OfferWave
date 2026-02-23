package com.offernow.interceptor;

import com.offernow.entity.User;
import com.offernow.mapper.UserMapper;
import com.offernow.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String jwtToken = resolveToken(request);
        String userId = null;

        if (jwtToken != null) {
            try {
                userId = jwtUtil.getSubjectFromToken(jwtToken);
            } catch (Exception ignored) {
                userId = null;
            }
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                User user = userMapper.selectById(Long.valueOf(userId));
                if (user != null && jwtUtil.validateToken(jwtToken, user.getId().toString())) {
                    List<SimpleGrantedAuthority> authorities =
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (NumberFormatException ignored) {
                // userId in token is not a valid number
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            // Standard form: Authorization: Bearer <token>
            if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = authorization.substring(7).trim();
                // Be tolerant of mistaken input like "Bearer Bearer <token>" in API docs UI.
                if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    token = token.substring(7).trim();
                }
                return token.isEmpty() ? null : token;
            }
            // Compatibility form: Authorization: <token>
            String token = authorization.trim();
            return token.isEmpty() ? null : token;
        }

        // Legacy compatibility: token: <token>
        String fallbackToken = request.getHeader("token");
        if (fallbackToken != null && !fallbackToken.isBlank()) {
            String token = fallbackToken.trim();
            return token.isEmpty() ? null : token;
        }

        // Query fallback for clients that append token in URL.
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }

        // Cookie fallback for browser clients.
        String cookieToken = resolveTokenFromCookies(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        return null;
    }

    private String resolveTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("token".equalsIgnoreCase(cookie.getName()) && cookie.getValue() != null) {
                String token = cookie.getValue().trim();
                return token.isEmpty() ? null : token;
            }
            if ("Authorization".equalsIgnoreCase(cookie.getName()) && cookie.getValue() != null) {
                String value = cookie.getValue().trim();
                if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    value = value.substring(7).trim();
                }
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}
