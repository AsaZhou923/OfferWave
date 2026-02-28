package com.offernow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdminJobUpsertDto implements Serializable {

    private Long id;

    @NotBlank
    private String companyName;

    @NotBlank
    private String companyType;

    private String companyBusiness;

    @NotBlank
    private String jobTitle;

    @NotBlank
    private String city;

    @NotBlank
    private String recruitType;

    private String targetAudience;

    private String announcement;

    private String salaryRange;

    private Integer salaryMin;

    private Integer salaryMax;

    private String education;

    private String applyLink;

    private String testInfo;

    private String processStage;

    private String deadline;

    private String sourceOrigin;

    private Integer auditStatus;
}
