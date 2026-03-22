package com.offerwave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "管理员新增/编辑职位请求参数")
public class AdminJobUpsertDto implements Serializable {

    @Schema(description = "职位 ID（新增时可不传，编辑时由路径参数决定）")
    private Long id;

    @NotBlank
    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @NotBlank
    @Schema(description = "公司类型（国企/外企/民企/上市公司）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyType;

    @Schema(description = "所属行业")
    private String companyBusiness;

    @NotBlank
    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobTitle;

    @NotBlank
    @Schema(description = "工作城市", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @NotBlank
    @Schema(description = "招聘类型（春招/秋招/实习）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recruitType;

    @Schema(description = "招聘对象（如：2026届）")
    private String targetAudience;

    @Schema(description = "职位公告/描述")
    private String announcement;

    @Schema(description = "薪资范围文本（如：15k-25k）")
    private String salaryRange;

    @Schema(description = "最低薪资（数值）")
    private Integer salaryMin;

    @Schema(description = "最高薪资（数值）")
    private Integer salaryMax;

    @Schema(description = "学历要求")
    private String education;

    @Schema(description = "投递链接")
    private String applyLink;

    @Schema(description = "笔试信息")
    private String testInfo;

    @Schema(description = "招聘流程阶段")
    private String processStage;

    @Schema(description = "截止日期（YYYY-MM-DD）")
    private String deadline;

    @Schema(description = "数据来源")
    private String sourceOrigin;

    @Schema(description = "审核状态：0=待审，1=已上线，2=驳回")
    private Integer auditStatus;
}
