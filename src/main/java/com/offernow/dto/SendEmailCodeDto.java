package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "发送邮箱验证码请求参数")
public class SendEmailCodeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "验证码类型不能为空")
    @Pattern(regexp = "register|login|reset_pwd", message = "验证码类型仅支持 register、login 或 reset_pwd")
    @Schema(description = "验证码类型：register/login/reset_pwd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;
}
