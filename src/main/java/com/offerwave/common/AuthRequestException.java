package com.offerwave.common;

/**
 * An expected authentication-domain rejection with a client-safe response.
 * Infrastructure and programming failures must not be converted to this type.
 */
public class AuthRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    private AuthRequestException(int statusCode, String publicMessage) {
        super(publicMessage);
        this.statusCode = statusCode;
    }

    public static AuthRequestException badRequest(String publicMessage) {
        return new AuthRequestException(400, publicMessage);
    }

    public static AuthRequestException unauthorized(String publicMessage) {
        return new AuthRequestException(401, publicMessage);
    }

    public static AuthRequestException rateLimited() {
        return new AuthRequestException(429, "请求过于频繁，请稍后再试");
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getPublicMessage() {
        return getMessage();
    }
}
