package com.offernow.controller;

import com.offernow.common.R;
import com.offernow.dto.EmailCodeLoginDto;
import com.offernow.dto.RegisterDto;
import com.offernow.dto.ResetPasswordDto;
import com.offernow.dto.SendEmailCodeDto;
import com.offernow.dto.UsernamePasswordLoginDto;
import com.offernow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证模块", description = "处理用户认证相关能力")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "账号密码登录", description = "用户通过用户名和密码获取 JWT Token")
    public R<Map<String, Object>> login(@Validated @RequestBody UsernamePasswordLoginDto loginDto) {
        try {
            return R.success(authService.login(loginDto));
        } catch (Exception e) {
            return R.error(401, "登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户通过用户名和密码注册")
    public R<String> register(@Validated @RequestBody RegisterDto registerDto) {
        try {
            authService.register(registerDto);
            return R.success("注册成功");
        } catch (Exception e) {
            return R.error(400, "注册失败: " + e.getMessage());
        }
    }

    @PostMapping("/send-email-code")
    @Operation(summary = "发送邮箱验证码", description = "支持 login / reset_pwd 两类验证码发送")
    public R<String> sendEmailCode(@Validated @RequestBody SendEmailCodeDto dto, HttpServletRequest request) {
        try {
            authService.sendEmailCode(dto, resolveClientIp(request));
            return R.success("验证码发送成功");
        } catch (IllegalStateException e) {
            return R.error(429, e.getMessage());
        } catch (Exception e) {
            return R.error(400, "验证码发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/login/email")
    @Operation(summary = "邮箱验证码登录", description = "邮箱 + 验证码登录，未注册邮箱自动创建账号")
    public R<Map<String, Object>> loginByEmailCode(@Validated @RequestBody EmailCodeLoginDto dto) {
        try {
            return R.success(authService.loginByEmailCode(dto));
        } catch (Exception e) {
            return R.error(401, "登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/password/reset")
    @Operation(summary = "重置密码", description = "邮箱 + 验证码校验后更新密码")
    public R<String> resetPassword(@Validated @RequestBody ResetPasswordDto dto) {
        try {
            authService.resetPassword(dto);
            return R.success("密码重置成功");
        } catch (Exception e) {
            return R.error(400, "密码重置失败: " + e.getMessage());
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

