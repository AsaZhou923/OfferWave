package com.offernow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MaterialPackageDetailDto {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String title;

    private String slug;

    private String subtitle;

    private String excerpt;

    private String iconUrl;

    private String coverImageUrl;

    private String content;

    private String downloadTip;

    private Integer accessType;

    private Integer status;

    private Integer sortOrder;

    private Long viewCount;

    private Long downloadCount;

    private boolean requiresMembership;

    private boolean canDownload;

    private LocalDateTime publishedAt;

    private List<String> previewImages = new ArrayList<>();

    private List<MaterialCatalogItemDto> fileCatalog = new ArrayList<>();

    private List<MaterialDownloadItemDto> downloads = new ArrayList<>();
}
