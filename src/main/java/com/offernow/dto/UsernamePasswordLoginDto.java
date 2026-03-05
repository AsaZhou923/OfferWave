package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户名密码登录请求 DTO。
 */
@Data
@Schema(description = "账号密码登录请求参数")
public class UsernamePasswordLoginDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "用户名不能为空")
    @Schema(description = "登录账号（用户名或邮箱）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotEmpty(message = "密码不能为空")
    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
