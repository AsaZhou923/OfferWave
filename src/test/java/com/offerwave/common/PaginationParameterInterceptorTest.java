package com.offerwave.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationParameterInterceptorTest {

    private final PaginationParameterInterceptor interceptor = new PaginationParameterInterceptor();

    @Test
    void shouldAcceptBoundaryValues() {
        MockHttpServletRequest request = requestWith("1", "100");

        assertDoesNotThrow(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void shouldRejectPageBelowOne() {
        MockHttpServletRequest request = requestWith("0", "20");

        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void shouldRejectSizeAboveOneHundred() {
        MockHttpServletRequest request = requestWith("1", "101");

        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void shouldRejectNonNumericPaginationValues() {
        MockHttpServletRequest request = requestWith("first", "20");

        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    private static MockHttpServletRequest requestWith(String page, String size) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/jobs");
        request.addParameter("page", page);
        request.addParameter("size", size);
        return request;
    }
}
