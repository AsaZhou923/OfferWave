package com.offerwave.controller;

import com.offerwave.common.ApiResponseStatusAdvice;
import com.offerwave.common.AuthRequestException;
import com.offerwave.common.GlobalExceptionHandler;
import com.offerwave.common.R;
import com.offerwave.config.OfferWaveSecurityProperties;
import com.offerwave.dto.SendEmailCodeDto;
import com.offerwave.dto.UsernamePasswordLoginDto;
import com.offerwave.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;
    private OfferWaveSecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        controller = new AuthController();
        securityProperties = new OfferWaveSecurityProperties();
        ReflectionTestUtils.setField(controller, "authService", authService);
        ReflectionTestUtils.setField(controller, "securityProperties", securityProperties);
    }

    @Test
    void loginFailureShouldNotExposeInternalExceptionMessage() {
        UsernamePasswordLoginDto dto = new UsernamePasswordLoginDto();
        dto.setUsername("user@example.com");
        dto.setPassword("password");
        when(authService.login(dto)).thenThrow(
                AuthRequestException.unauthorized("用户名或密码错误"));

        R<Map<String, Object>> response = controller.login(dto);

        assertEquals(401, response.getCode());
        assertFalse(response.getMessage().contains("secret-db-host"));
    }

    @Test
    void shouldIgnoreForwardedHeaderFromUntrustedRemoteAddress() {
        SendEmailCodeDto dto = sendCodeDto();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "198.51.100.7");

        controller.sendEmailCode(dto, request);

        verify(authService).sendEmailCode(dto, "203.0.113.9");
    }

    @Test
    void shouldUseForwardedHeaderOnlyForExplicitlyTrustedProxy() {
        securityProperties.setTrustedProxyAddresses(List.of("10.0.0.8"));
        SendEmailCodeDto dto = sendCodeDto();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.8");

        controller.sendEmailCode(dto, request);

        verify(authService).sendEmailCode(dto, "198.51.100.7");
    }

    @Test
    void unexpectedSendCodeFailureShouldReachSanitizedHttp500Boundary() throws Exception {
        doThrow(new RuntimeException("smtp password leaked"))
                .when(authService).sendEmailCode(any(), anyString());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ApiResponseStatusAdvice())
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/send-email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"type\":\"reset_pwd\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains("smtp password"));
    }

    private SendEmailCodeDto sendCodeDto() {
        SendEmailCodeDto dto = new SendEmailCodeDto();
        dto.setEmail("user@example.com");
        dto.setType("reset_pwd");
        return dto;
    }
}
