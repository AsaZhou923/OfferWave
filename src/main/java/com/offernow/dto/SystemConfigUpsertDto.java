package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "系统配置新增/更新请求参数")
public class SystemConfigUpsertDto implements Serializable {

    @NotBlank
    @Schema(description = "配置键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configKey;

    @NotBlank
    @Schema(description = "配置值", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configValue;

    @Schema(description = "配置描述")
    private String description;
}
