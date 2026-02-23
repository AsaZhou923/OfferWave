package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "爬虫批量上报招聘数据请求体")
public class CrawlerSyncDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "批次号 (如 \"crawl_20260218_01\")", requiredMode = Schema.RequiredMode.REQUIRED)
    private String batchId;

    @Schema(description = "职位数据列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CrawlerJobItemDto> items;
}
