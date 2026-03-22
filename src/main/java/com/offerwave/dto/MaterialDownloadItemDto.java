package com.offerwave.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialDownloadItemDto {

    private Long id;

    @NotBlank(message = "下载项标题不能为空")
    private String title;

    @NotBlank(message = "下载链接不能为空")
    private String downloadUrl;

    private String extractionCode;

    private String fileType;

    private String fileSize;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
