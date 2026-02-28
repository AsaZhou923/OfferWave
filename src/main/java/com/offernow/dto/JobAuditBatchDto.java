package com.offernow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class JobAuditBatchDto implements Serializable {

    @NotEmpty
    private List<Long> jobIds;

    /** 1-通过上线, 2-驳回 */
    @NotNull
    private Integer auditStatus;
}
