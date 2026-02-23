package com.offernow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位表实体。
 */
@TableName(value = "jobs")
@Data
public class Job implements Serializable {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司名称 */
    private String companyName;

    /** 公司类型 */
    private String companyType;

    /** 所属行业 */
    private String companyBusiness;

    /** 职位名称 */
    private String jobTitle;

    /** 工作城市 */
    private String city;

    /** 招聘类型 */
    private String recruitType;

    /** 招聘对象 */
    private String targetAudience;

    /** 职位公告 */
    private String announcement;

    /** 薪资范围文本 */
    private String salaryRange;

    /** 最低薪资 */
    private Integer salaryMin;

    /** 最高薪资 */
    private Integer salaryMax;

    /** 学历要求 */
    private String education;

    /** 投递链接 */
    private String applyLink;

    /** 笔试信息 */
    private String testInfo;

    /** 招聘进度 */
    private String processStage;

    /** 截止日期 */
    private String deadline;

    /** 去重哈希 */
    private String uniqueHash;

    /** 数据来源 */
    private String sourceOrigin;

    /** 审核状态（0:待审,1:上线,2:拒绝） */
    private Integer auditStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否临近截止（非持久化字段） */
    @TableField(exist = false)
    private Boolean isUrgent;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
