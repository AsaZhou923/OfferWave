package com.offerwave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "敏感词新增/更新请求参数")
public class SensitiveWordUpsertDto implements Serializable {

    @NotBlank
    @Schema(description = "敏感词内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String word;

    @Schema(description = "是否启用：1=启用，0=禁用")
    private Integer enabled = 1;
}
