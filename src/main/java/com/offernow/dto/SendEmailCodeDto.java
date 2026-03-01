package com.offernow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class SendEmailCodeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码类型不能为空")
    @Pattern(regexp = "login|reset_pwd", message = "验证码类型仅支持 login 或 reset_pwd")
    private String type;
}

