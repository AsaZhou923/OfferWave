package com.offerwave.interceptor;

import com.offerwave.mapper.UserMapper;
import com.offerwave.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();

    @Test
    void shouldResolveOnlyStandardAuthorizationBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc123");

        assertEquals("abc123", resolveToken(request));
    }

    @Test
    void shouldRejectDuplicateBearerPrefix() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer Bearer abc123");

        assertNull(resolveToken(request));
    }

    @Test
    void shouldIgnoreLegacyHeaderQueryAndCookies() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("token", "legacy-token");
        request.setParameter("token", "query-token");
        request.setCookies(
                new Cookie("Authorization", "Bearer cookie-token"),
                new Cookie("token", "cookie-token"));

        assertNull(resolveToken(request));
    }

    @Test
    void shouldRejectRawAuthorizationToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "abc123");

        assertNull(resolveToken(request));
    }

    @Test
    void shouldReturnNullWhenAuthorizationBearerContainsNoToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   ");

        assertNull(resolveToken(request));
    }

    @Test
    void databaseFailureShouldPropagateInsteadOfBeingMaskedAsUnauthenticated() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserMapper userMapper = mock(UserMapper.class);
        FilterChain chain = mock(FilterChain.class);
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(filter, "userMapper", userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer signed-token");
        when(jwtUtil.getSubjectFromToken("signed-token")).thenReturn("42");
        when(userMapper.selectById(42L)).thenThrow(new RuntimeException("database unavailable"));

        assertThrows(RuntimeException.class,
                () -> filter.doFilterInternal(request, new MockHttpServletResponse(), chain));
    }

    private String resolveToken(MockHttpServletRequest request) throws Exception {
        Method method = JwtAuthenticationFilter.class.getDeclaredMethod(
                "resolveToken", jakarta.servlet.http.HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(filter, request);
    }
}
