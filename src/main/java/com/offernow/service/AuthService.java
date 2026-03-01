package com.offernow.service;

import com.offernow.dto.EmailCodeLoginDto;
import com.offernow.dto.RegisterDto;
import com.offernow.dto.ResetPasswordDto;
import com.offernow.dto.SendEmailCodeDto;
import com.offernow.dto.UsernamePasswordLoginDto;

import java.util.Map;

/**
 * 认证服务接口。
 */
public interface AuthService {

    /**
     * 用户名密码登录。
     *
     * @param loginDto 登录参数
     * @return 包含 token 与用户信息的结果
     */
    Map<String, Object> login(UsernamePasswordLoginDto loginDto);

    /**
     * 用户注册。
     *
     * @param registerDto 注册参数
     */
    void register(RegisterDto registerDto);

    /**
     * 发送邮箱验证码。
     *
     * @param dto 业务参数（邮箱 + 类型）
     * @param clientIp 客户端 IP
     */
    void sendEmailCode(SendEmailCodeDto dto, String clientIp);

    /**
     * 邮箱验证码登录（支持验证码即注册）。
     *
     * @param dto 登录参数
     * @return 包含 token 与用户信息
     */
    Map<String, Object> loginByEmailCode(EmailCodeLoginDto dto);

    /**
     * 重置密码。
     *
     * @param dto 邮箱 + 验证码 + 新密码
     */
    void resetPassword(ResetPasswordDto dto);
}
