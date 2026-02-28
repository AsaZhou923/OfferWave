package com.offernow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class SensitiveWordUpsertDto implements Serializable {

    @NotBlank
    private String word;

    private Integer enabled = 1;
}
