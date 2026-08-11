package com.offerwave.interceptor;

import com.offerwave.entity.User;
import com.offerwave.mapper.UserMapper;
import com.offerwave.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
 * Authenticates only standards-compliant Authorization: Bearer tokens.
 */
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
            } catch (JwtException | IllegalArgumentException ignored) {
                userId = null;
            }
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                User user = userMapper.selectById(Long.valueOf(userId));
                if (user != null
                        && !Integer.valueOf(0).equals(user.getAccountStatus())
                        && jwtUtil.validateToken(jwtToken, user)) {
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
                // A non-numeric subject is an invalid credential, not a system failure.
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty() || token.chars().anyMatch(Character::isWhitespace)) {
            return null;
        }
        return token;
    }
}
