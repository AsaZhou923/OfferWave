package com.offernow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "offernow.storage")
public class StorageProperties {

    private String rootPath = Paths.get(System.getProperty("user.dir"), "storage").toString();

    private String publicUrlPrefix = "/uploads";

    private long materialImageMaxSize = 10L * 1024 * 1024;

    public Path resolveRootPath() {
        return Paths.get(rootPath).toAbsolutePath().normalize();
    }

    public String resolvePublicUrlPrefix() {
        if (publicUrlPrefix == null || publicUrlPrefix.isBlank()) {
            return "/uploads";
        }
        String normalized = publicUrlPrefix.startsWith("/") ? publicUrlPrefix : "/" + publicUrlPrefix;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    public String resolvePublicPattern() {
        return resolvePublicUrlPrefix() + "/**";
    }

    public String resolveRootLocation() {
        String location = resolveRootPath().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
