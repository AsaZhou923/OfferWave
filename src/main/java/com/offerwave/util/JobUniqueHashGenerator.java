package com.offerwave.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Canonical server-side identity for job de-duplication.
 */
public final class JobUniqueHashGenerator {

    private static final String SEPARATOR = "|";

    private JobUniqueHashGenerator() {
    }

    public static String generate(String companyName, String jobTitle, String city) {
        String identity = normalize(companyName)
                + SEPARATOR + normalize(jobTitle)
                + SEPARATOR + normalize(city);
        return DigestUtils.md5DigestAsHex(identity.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("job identity fields must not be null");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
