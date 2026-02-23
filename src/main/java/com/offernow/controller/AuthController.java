package com.offernow.controller;

import com.offernow.common.R;
import com.offernow.dto.RegisterDto;
import com.offernow.dto.UsernamePasswordLoginDto;
import com.offernow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证模块控制器。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证模块", description = "处理用户注册与登录")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户名密码登录。
     */
    @PostMapping("/login")
    @Operation(summary = "用户名密码登录", description = "用户通过用户名和密码获取 JWT Token")
    public R<Map<String, Object>> login(@Validated @RequestBody UsernamePasswordLoginDto loginDto) {
        try {
            Map<String, Object> responseData = authService.login(loginDto);
            return R.success(responseData);
        } catch (Exception e) {
            return R.error(401, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户通过用户名和密码进行注册")
    public R<String> register(@Validated @RequestBody RegisterDto registerDto) {
        try {
            authService.register(registerDto);
            return R.success("注册成功");
        } catch (Exception e) {
            return R.error(400, "注册失败: " + e.getMessage());
        }
    }
}
