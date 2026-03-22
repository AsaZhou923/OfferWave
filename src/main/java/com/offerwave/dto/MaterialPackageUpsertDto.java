package com.offerwave.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MaterialPackageUpsertDto {

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "资料包标题不能为空")
    private String title;

    @NotBlank(message = "资料包标识不能为空")
    private String slug;

    private String subtitle;

    private String iconUrl;

    private String coverImageUrl;

    private String excerpt;

    @NotBlank(message = "正文内容不能为空")
    private String content;

    private String downloadTip;

    private Integer accessType;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime publishedAt;

    private List<String> previewImages = new ArrayList<>();

    @Valid
    private List<MaterialCatalogItemDto> fileCatalog = new ArrayList<>();

    @Valid
    private List<MaterialDownloadItemDto> downloads = new ArrayList<>();
}
