package com.offernow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 职位信息主表
 */
@TableName(value ="jobs")
@Data
public class Job implements Serializable {
    /**
     * 主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公司名称 (索引)
     */
    private String companyName;

    /**
     * 公司类型 (国企/外企等)
     */
    private String companyType;
    
    /**
     * 公司所属行业(互联网/电商等)
     */
    private String companyBusiness;

    /**
     * 岗位名称 (索引，支持模糊搜索)
     */
    private String jobTitle;

    /**
     * 工作地点 (索引)
     */
    private String city;

    /**
     * 招聘类型 (春招/秋招/实习)
     */
    private String recruitType;

    /**
     * 招聘对象 (如：2026届)
     */
    private String targetAudience;

    /**
     * 招聘公告
     */
    private String announcement;

    /**
     * 薪资范围 (用于高级筛选)
     */
    private String salaryRange;

    /**
     * 最低薪资(元/月) - 用于范围筛选
     */
    private Integer salaryMin;

    /**
     * 最高薪资(元/月) - 用于范围筛选
     */
    private Integer salaryMax;

    /**
     * 学历要求 (本科/硕士等)
     */
    private String education;

    /**
     * 投递链接
     */
    private String applyLink;

    /**
     * 笔试情况
     */
    private String testInfo;

    /**
     * 全局招聘进度
     */
    private String processStage;

    /**
     * 截止日期 (日期或招满即止)
     */
    private String deadline;

    /**
     * 爬虫去重哈希 (MD5)
     */
    private String uniqueHash;

    /**
     * 数据来源 (爬虫/人工/用户投稿)
     */
    private String sourceOrigin;

    /**
     * 审核状态 (0:待审, 1:上线, 2:拒绝)
     */
    private Integer auditStatus;

    /**
     * 入库时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 (用于排序)
     */
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Boolean isUrgent;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
