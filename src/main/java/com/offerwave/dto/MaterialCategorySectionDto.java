package com.offerwave.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MaterialCategorySectionDto {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private Integer sortOrder;

    private List<MaterialPackageCardDto> packages = new ArrayList<>();
}
