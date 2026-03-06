package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求 DTO。
 */
@Data
@Schema(description = "用户注册请求参数")
public class RegisterDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotEmpty(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须为6位数字")
    @Schema(description = "邮箱验证码（6位数字）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotEmpty(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少 6 位")
    @Schema(description = "登录密码（至少6位）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
