package com.offerwave.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Security-sensitive deployment settings that must be explicitly configured.
 */
@Component
@ConfigurationProperties(prefix = "offerwave.security")
public class OfferWaveSecurityProperties {

    private List<String> trustedProxyAddresses = new ArrayList<>();
    private List<String> corsAllowedOrigins = new ArrayList<>();

    @PostConstruct
    public void validate() {
        trustedProxyAddresses = normalize(trustedProxyAddresses);
        corsAllowedOrigins = normalize(corsAllowedOrigins);
        if (corsAllowedOrigins.contains("*")) {
            throw new IllegalStateException(
                    "offerwave.security.cors-allowed-origins must contain explicit origins; wildcard '*' is forbidden");
        }
    }

    public boolean isTrustedProxy(String remoteAddress) {
        return StringUtils.hasText(remoteAddress) && trustedProxyAddresses.contains(remoteAddress.trim());
    }

    public List<String> getTrustedProxyAddresses() {
        return List.copyOf(trustedProxyAddresses);
    }

    public void setTrustedProxyAddresses(List<String> trustedProxyAddresses) {
        this.trustedProxyAddresses = trustedProxyAddresses == null
                ? new ArrayList<>()
                : new ArrayList<>(trustedProxyAddresses);
    }

    public List<String> getCorsAllowedOrigins() {
        return List.copyOf(corsAllowedOrigins);
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins == null
                ? new ArrayList<>()
                : new ArrayList<>(corsAllowedOrigins);
    }

    private List<String> normalize(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }
}
