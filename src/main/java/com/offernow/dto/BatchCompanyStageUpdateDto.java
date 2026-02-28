package com.offernow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class BatchCompanyStageUpdateDto implements Serializable {

    @NotBlank
    private String companyName;

    @NotBlank
    private String processStage;
}
