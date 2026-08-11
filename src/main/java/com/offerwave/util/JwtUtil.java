package com.offerwave.util;

import com.offerwave.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT issuance and validation with a deployment-provided Base64 HMAC key.
 */
@Component
public final class JwtUtil {

    private static final String CREDENTIAL_VERSION_CLAIM = "cv";
    private static final int MINIMUM_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final long expiration;

    public JwtUtil(
            @Value("${offerwave.jwt.secret:}") String encodedSecret,
            @Value("${offerwave.jwt.expiration:86400000}") Long expiration) {
        this.signingKey = decodeSigningKey(encodedSecret);
        if (expiration == null || expiration <= 0) {
            throw new IllegalStateException("offerwave.jwt.expiration must be a positive number of milliseconds");
        }
        this.expiration = expiration;
    }

    public String getSubjectFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Cannot issue a token without a persisted user ID");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put(CREDENTIAL_VERSION_CLAIM, credentialVersion(user.getPasswordHash()));
        return doGenerateToken(claims, user.getId().toString());
    }

    /**
     * Changing a password changes the credential-version claim, invalidating
     * all tokens issued for the previous password without extra server state.
     */
    public boolean validateToken(String token, User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        try {
            Claims claims = getAllClaimsFromToken(token);
            String tokenVersion = claims.get(CREDENTIAL_VERSION_CLAIM, String.class);
            String currentVersion = credentialVersion(user.getPasswordHash());
            return user.getId().toString().equals(claims.getSubject())
                    && tokenVersion != null
                    && MessageDigest.isEqual(
                            tokenVersion.getBytes(StandardCharsets.UTF_8),
                            currentVersion.getBytes(StandardCharsets.UTF_8));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        Date createdDate = new Date();
        Date expirationDate = new Date(createdDate.getTime() + expiration);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private String credentialVersion(String passwordHash) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] digest = mac.doFinal(
                    (passwordHash == null ? "" : passwordHash).getBytes(StandardCharsets.UTF_8));
            return Encoders.BASE64URL.encode(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to derive JWT credential version", ex);
        }
    }

    private SecretKey decodeSigningKey(String encodedSecret) {
        if (!StringUtils.hasText(encodedSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is required and must be Base64 encoding of at least 32 random bytes");
        }
        final byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(encodedSecret.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "JWT_SECRET must be valid Base64 encoding of at least 32 random bytes", ex);
        }
        if (keyBytes.length < MINIMUM_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
