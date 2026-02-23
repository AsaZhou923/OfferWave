package com.offernow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级配置表实体。
 */
@TableName(value = "memberships")
@Data
public class Membership implements Serializable {

    /** 主键 ID */
    private Integer id;

    /** 等级名称 */
    private String levelName;

    /** 价格 */
    private BigDecimal price;

    /** 有效期天数（-1 表示永久） */
    private Integer durationDays;

    /** 权益配置 JSON */
    private String privileges;

    /** 等级图标地址 */
    private String iconUrl;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
