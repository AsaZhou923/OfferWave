package com.offerwave.util;

import com.offerwave.entity.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    @Test
    void shouldRejectMissingOrShortBase64Secret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("", 60_000L));
        assertThrows(IllegalStateException.class,
                () -> new JwtUtil(Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8)),
                        60_000L));
    }

    @Test
    void passwordChangeShouldRevokePreviouslyIssuedToken() {
        JwtUtil jwtUtil = new JwtUtil(validSecret(), 60_000L);
        User user = userWithPassword("$2a$10$old-password-hash");

        String token = jwtUtil.generateToken(user);
        assertTrue(jwtUtil.validateToken(token, user));

        user.setPasswordHash("$2a$10$new-password-hash");
        assertFalse(jwtUtil.validateToken(token, user));
    }

    private User userWithPassword(String passwordHash) {
        User user = new User();
        user.setId(42L);
        user.setPasswordHash(passwordHash);
        return user;
    }

    private String validSecret() {
        return Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    }
}
