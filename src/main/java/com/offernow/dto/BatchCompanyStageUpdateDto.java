package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "按公司批量更新招聘阶段请求参数")
public class BatchCompanyStageUpdateDto implements Serializable {

    @NotBlank
    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @NotBlank
    @Schema(description = "要更新到的招聘流程阶段", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processStage;
}
