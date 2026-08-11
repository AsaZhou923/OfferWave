package com.offerwave.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiBoundaryWebTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BoundaryTestController())
                .setControllerAdvice(new GlobalExceptionHandler(), new ApiResponseStatusAdvice())
                .addInterceptors(new PaginationParameterInterceptor())
                .build();
    }

    @Test
    void controllerErrorEnvelopeShouldSetRealHttpStatus() throws Exception {
        mockMvc.perform(get("/api/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资料不存在"));
    }

    @Test
    void paginationInterceptorShouldProduceBadRequestEnvelope() throws Exception {
        mockMvc.perform(get("/api/test/items").param("page", "1").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("100")));
    }

    @Test
    void unhandledExceptionShouldReturnSanitizedServerError() throws Exception {
        mockMvc.perform(get("/api/test/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                .andExpect(jsonPath("$.message").value(not(containsString("database-password"))));
    }

    @RestController
    private static class BoundaryTestController {

        @GetMapping("/api/test/not-found")
        R<String> notFound() {
            return R.error(404, "资料不存在");
        }

        @GetMapping("/api/test/items")
        R<String> items() {
            return R.success("ok");
        }

        @GetMapping("/api/test/failure")
        R<String> failure() {
            throw new IllegalStateException("database-password=secret");
        }
    }
}
