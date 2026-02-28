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
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：
 * 1) 从 Header / Query / Cookie 中解析 Token
 * 2) 校验 Token 合法性与过期时间
 * 3) 将认证后的用户信息写入 Spring Security 上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类，用于解析与校验 Token */
    @Autowired
    private JwtUtil jwtUtil;

    /** 用户数据访问，用于根据 Token 中的用户 ID 查询用户 */
    @Autowired
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 第一步：解析请求中的 Token
        String jwtToken = resolveToken(request);
        String userId = null;

        if (jwtToken != null) {
            try {
                // 从 Token 的 subject 中读取用户 ID
                userId = jwtUtil.getSubjectFromToken(jwtToken);
            } catch (Exception ignored) {
                userId = null;
            }
        }

        // 第二步：若当前请求尚未认证，则尝试完成认证流程
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                User user = userMapper.selectById(Long.valueOf(userId));
                if (user != null
                        && !Integer.valueOf(0).equals(user.getAccountStatus())
                        && jwtUtil.validateToken(jwtToken, user.getId().toString())) {
                    // 构造认证信息并写入 SecurityContext，后续接口即可识别已登录用户
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    if (Integer.valueOf(1).equals(user.getRole())) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (NumberFormatException ignored) {
                // Token 中的 userId 不是有效数字，忽略并按未登录处理
            }
        }

        // 第三步：继续执行后续过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 多来源解析 Token，兼容新旧客户端。
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            // 标准形式：Authorization: Bearer <token>
            if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = authorization.substring(7).trim();
                // 兼容误填为 "Bearer Bearer <token>" 的情况
                if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    token = token.substring(7).trim();
                }
                return token.isEmpty() ? null : token;
            }
            // 兼容形式：Authorization: <token>
            String token = authorization.trim();
            return token.isEmpty() ? null : token;
        }

        // 兼容旧客户端：token: <token>
        String fallbackToken = request.getHeader("token");
        if (fallbackToken != null && !fallbackToken.isBlank()) {
            String token = fallbackToken.trim();
            return token.isEmpty() ? null : token;
        }

        // 兼容 URL Query 传参：?token=xxx
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }

        // 兼容浏览器 Cookie 透传
        String cookieToken = resolveTokenFromCookies(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        return null;
    }

    /**
     * 从 Cookie 中提取 Token。
     */
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
