package com.offernow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * User registration request DTO.
 */
@Data
public class RegisterDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotEmpty(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须为6位数字")
    private String code;

    @NotEmpty(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少 6 位")
    private String password;
}
