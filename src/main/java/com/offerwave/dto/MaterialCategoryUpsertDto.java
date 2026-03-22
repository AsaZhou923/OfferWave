package com.offerwave.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialCategoryUpsertDto {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @NotBlank(message = "分类标识不能为空")
    private String slug;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
