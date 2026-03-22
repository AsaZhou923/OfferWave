package com.offerwave.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialPackageCardDto {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String title;

    private String slug;

    private String subtitle;

    private String excerpt;

    private String iconUrl;

    private String coverImageUrl;

    private Integer accessType;

    private Long viewCount;

    private Integer sortOrder;

    private LocalDateTime publishedAt;
}
