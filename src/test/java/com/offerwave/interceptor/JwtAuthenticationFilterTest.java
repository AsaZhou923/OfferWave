package com.offerwave.interceptor;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();

    @Test
    void shouldResolveTokenFromAuthorizationBearerWithDuplicatePrefix() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer Bearer abc123");

        assertEquals("abc123", resolveToken(request));
    }

    @Test
    void shouldResolveTokenFromLegacyTokenHeaderWhenAuthorizationMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("token", "legacy-token");

        assertEquals("legacy-token", resolveToken(request));
    }

    @Test
    void shouldResolveTokenFromAuthorizationCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("Authorization", "Bearer cookie-token"));

        assertEquals("cookie-token", resolveToken(request));
    }

    @Test
    void shouldResolveTokenFromTokenCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", "cookie-token"));

        assertEquals("cookie-token", resolveToken(request));
    }

    @Test
    void shouldReturnNullWhenAuthorizationBearerContainsNoToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   ");

        assertNull(resolveToken(request));
    }

    private String resolveToken(MockHttpServletRequest request) throws Exception {
        Method method = JwtAuthenticationFilter.class.getDeclaredMethod("resolveToken", jakarta.servlet.http.HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(filter, request);
    }
}
