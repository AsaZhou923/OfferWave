package com.offernow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求 DTO。
 */
@Data
public class RegisterDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度必须在 4 到 50 个字符之间")
    private String username;

    @NotEmpty(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少 6 位")
    private String password;
}
