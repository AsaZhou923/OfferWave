package com.offerwave.service;

import com.offerwave.dto.EmailCodeLoginDto;
import com.offerwave.dto.RegisterDto;
import com.offerwave.dto.ResetPasswordDto;
import com.offerwave.dto.SendEmailCodeDto;
import com.offerwave.dto.UsernamePasswordLoginDto;

import java.util.Map;

/**
 * Authentication service interface.
 */
public interface AuthService {

    /**
     * Username/email + password login.
     *
     * @param loginDto login parameters
     * @return token and user profile
     */
    Map<String, Object> login(UsernamePasswordLoginDto loginDto);

    /**
     * Register with email + password + register code.
     *
     * @param registerDto registration parameters
     */
    void register(RegisterDto registerDto);

    /**
     * Send email code for register/login/reset.
     *
     * @param dto business parameters
     * @param clientIp client IP
     */
    void sendEmailCode(SendEmailCodeDto dto, String clientIp);

    /**
     * Login by email code (registered users only).
     *
     * @param dto login parameters
     * @return token and user profile
     */
    Map<String, Object> loginByEmailCode(EmailCodeLoginDto dto);

    /**
     * Reset password by email code.
     *
     * @param dto reset parameters
     */
    void resetPassword(ResetPasswordDto dto);
}
