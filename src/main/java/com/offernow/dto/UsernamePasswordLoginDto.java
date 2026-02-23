package com.offernow.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户名密码登录请求 DTO。
 */
@Data
public class UsernamePasswordLoginDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "用户名不能为空")
    private String username;

    @NotEmpty(message = "密码不能为空")
    private String password;
}
