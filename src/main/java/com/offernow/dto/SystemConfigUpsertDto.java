package com.offernow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class SystemConfigUpsertDto implements Serializable {

    @NotBlank
    private String configKey;

    @NotBlank
    private String configValue;

    private String description;
}
