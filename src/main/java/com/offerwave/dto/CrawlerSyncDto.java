package com.offerwave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 爬虫批量同步请求 DTO。
 */
@Data
@Schema(description = "爬虫批量上报招聘数据请求体")
public class CrawlerSyncDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "批次号（例如：crawl_20260218_01）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String batchId;

    @Schema(description = "职位数据列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Size(max = 1000)
    private List<CrawlerJobItemDto> items;
}
