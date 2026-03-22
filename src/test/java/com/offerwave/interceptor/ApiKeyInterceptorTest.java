package com.offerwave.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyInterceptorTest {

    private ApiKeyInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ApiKeyInterceptor();
    }

    @Test
    void shouldReturn503WhenApiKeyConfigMissing() throws Exception {
        ReflectionTestUtils.setField(interceptor, "configuredApiKey", "");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(503, response.getStatus());
    }

    @Test
    void shouldReturn401WhenHeaderMissing() throws Exception {
        ReflectionTestUtils.setField(interceptor, "configuredApiKey", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldPassWhenHeaderMatches() throws Exception {
        ReflectionTestUtils.setField(interceptor, "configuredApiKey", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }
}
