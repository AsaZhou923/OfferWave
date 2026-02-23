package com.offernow.service;

import com.offernow.dto.RegisterDto;
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
}
