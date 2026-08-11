package com.offerwave.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigTest {

    @Test
    void corsShouldAllowOnlyConfiguredOriginsWithoutCredentials() {
        OfferWaveSecurityProperties properties = new OfferWaveSecurityProperties();
        properties.setCorsAllowedOrigins(List.of("https://app.offerwave.example"));
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "securityProperties", properties);

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertEquals(List.of("https://app.offerwave.example"), cors.getAllowedOrigins());
        assertFalse(Boolean.TRUE.equals(cors.getAllowCredentials()));
        assertEquals("https://app.offerwave.example", cors.checkOrigin("https://app.offerwave.example"));
        assertNull(cors.checkOrigin("https://evil.example"));
    }

    @Test
    void wildcardCorsOriginShouldBeRejected() {
        OfferWaveSecurityProperties properties = new OfferWaveSecurityProperties();
        properties.setCorsAllowedOrigins(List.of("*"));

        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void openApiShouldAdvertiseOnlyBearerAuthentication() {
        var securitySchemes = new BeanConfig().customOpenAPI().getComponents().getSecuritySchemes();

        assertEquals(List.of("Authorization"), securitySchemes.keySet().stream().toList());
    }

    @Test
    void securityListsShouldBindFromCommaSeparatedDeploymentProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("offerwave.security.trusted-proxy-addresses", "10.0.0.8,10.0.0.9")
                .withProperty(
                        "offerwave.security.cors-allowed-origins",
                        "https://app.offerwave.example,https://admin.offerwave.example");

        OfferWaveSecurityProperties properties = Binder.get(environment)
                .bind("offerwave.security", Bindable.of(OfferWaveSecurityProperties.class))
                .orElseThrow(() -> new AssertionError("security properties did not bind"));
        properties.validate();

        assertEquals(List.of("10.0.0.8", "10.0.0.9"), properties.getTrustedProxyAddresses());
        assertEquals(
                List.of("https://app.offerwave.example", "https://admin.offerwave.example"),
                properties.getCorsAllowedOrigins());
    }
}
