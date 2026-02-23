package com.offernow.service;

import com.offernow.dto.RegisterDto;
import com.offernow.dto.UsernamePasswordLoginDto;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户名密码登录处理
     * @param loginDto 包含用户名和密码的登录数据
     * @return 包含 token 和用户信息的 Map
     */
    Map<String, Object> login(UsernamePasswordLoginDto loginDto);

    /**
     * 用户注册处理
     * @param registerDto 包含用户名和密码的注册数据
     */
    void register(RegisterDto registerDto);
}
