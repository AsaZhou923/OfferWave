package com.offerwave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "职位审核批量操作请求参数")
public class JobAuditBatchDto implements Serializable {

    @NotEmpty
    @Schema(description = "职位 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> jobIds;

    @NotNull
    @Schema(description = "审核状态：1=通过并上线，2=驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer auditStatus;
}
