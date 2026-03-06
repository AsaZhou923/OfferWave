package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户状态更新请求参数")
public class UserStatusUpdateDto implements Serializable {

    @NotNull
    @Schema(description = "账号状态：1=正常，0=封禁", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer accountStatus;
}
