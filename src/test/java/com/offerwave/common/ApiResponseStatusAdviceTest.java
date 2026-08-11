package com.offerwave.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseStatusAdviceTest {

    private final ResponseBodyAdvice<Object> advice = new ApiResponseStatusAdvice();

    @Test
    void errorCodeShouldSetMatchingHttpStatus() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        R<Object> body = R.error(404, "资料不存在");

        advice.beforeBodyWrite(
                body,
                null,
                null,
                MappingJackson2HttpMessageConverter.class,
                null,
                new ServletServerHttpResponse(servletResponse)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), servletResponse.getStatus());
        assertEquals("资料不存在", body.getMessage());
    }

    @Test
    void serverErrorShouldNotExposeInternalExceptionDetails() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        R<Object> body = R.error(500, "SQLException: password=secret");
        body.setData("debug payload");
        body.add("trace", "sensitive trace");

        advice.beforeBodyWrite(
                body,
                null,
                null,
                MappingJackson2HttpMessageConverter.class,
                null,
                new ServletServerHttpResponse(servletResponse)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), servletResponse.getStatus());
        assertEquals("服务器内部错误", body.getMessage());
        assertNull(body.getData());
        assertEquals(0, body.getMap().size());
    }
}
