package com.offerwave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位表实体。
 */
@TableName(value = "jobs")
@Data
@Schema(description = "职位信息")
public class Job implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "职位 ID")
    private Long id;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "公司类型")
    private String companyType;

    @Schema(description = "所属行业")
    private String companyBusiness;

    @TableField(exist = false)
    @Schema(description = "行业（兼容字段，值同 companyBusiness）")
    private String industry;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "工作城市")
    private String city;

    @Schema(description = "招聘类型（春招/秋招/实习）")
    private String recruitType;

    @Schema(description = "招聘对象")
    private String targetAudience;

    @Schema(description = "职位公告/描述")
    private String announcement;

    @Schema(description = "薪资范围文本")
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

    @Schema(description = "去重哈希")
    @TableField(value = "unique_hash", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String uniqueHash;

    @Schema(description = "数据来源")
    private String sourceOrigin;

    @Schema(description = "审核状态（0:待审, 1:上线, 2:驳回）")
    private Integer auditStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    @Schema(description = "是否临近截止（非持久化字段）")
    private Boolean isUrgent;

    @TableField(exist = false)
    @Schema(description = "当前用户是否已收藏该职位（未登录时默认 false）")
    private Boolean isCollected;

    @TableField(exist = false)
    @Schema(description = "当前用户投递状态码（0:未投递, 1:已投递, 2:笔试中, 3:面试中, 4:已录用, 5:流程结束；未登录时默认 0）")
    private Integer deliveryStatus;

    @TableField(exist = false)
    @Schema(description = "当前用户投递状态文案（未登录时默认“未投递”）")
    private String deliveryStatusStr;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
