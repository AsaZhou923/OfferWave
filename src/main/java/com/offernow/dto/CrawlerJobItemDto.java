package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 爬虫上报的单条职位数据 DTO。
 */
@Data
@Schema(description = "爬虫上报的单条职位数据模型")
public class CrawlerJobItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "核心去重字段（MD5: 公司名_职位名_城市）")
    private String uniqueHash;

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @Schema(description = "公司类型（国企/外企/民企/上市公司）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyType;

    @Schema(description = "所属行业")
    private String companyBusiness;

    @Schema(description = "职位名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobTitle;

    @Schema(description = "工作地点", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @Schema(description = "招聘类型（春招/秋招/实习）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recruitType;

    @Schema(description = "招聘对象（如：2026届）")
    private String targetAudience;

    @Schema(description = "学历要求")
    private String education;

    @Schema(description = "薪资范围文本")
    private String salaryRange;

    @Schema(description = "最低薪资（数值）")
    private Integer salaryMin;

    @Schema(description = "最高薪资（数值）")
    private Integer salaryMax;

    @Schema(description = "投递链接")
    private String applyLink;

    @Schema(description = "截止日期（YYYY-MM-DD）")
    private String deadline;

    @Schema(description = "招聘进度")
    private String processStage;

    @Schema(description = "笔试信息")
    private String testInfo;

    @Schema(description = "数据来源")
    private String sourceOrigin;
}
