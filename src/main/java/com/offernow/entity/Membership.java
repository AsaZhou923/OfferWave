package com.offernow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员等级配置表
 */
@TableName(value ="memberships")
@Data
public class Membership implements Serializable {
    /**
     * 主键 ID (1: 普通用户, 2: VIP会员)
     */
    private Integer id;

    /**
     * 等级名称 (如：“普通用户”、“VIP会员”)
     */
    private String levelName;

    /**
     * 价格 (单位：元，0 表示免费)
     */
    private BigDecimal price;

    /**
     * 有效期天数 (30代表月卡, 365代表年卡, -1代表永久)
     */
    private Integer durationDays;

    /**
     * 权益配置 (存储具体权限：如最大追踪数、是否可看分析等)
     */
    private String privileges;

    /**
     * 等级图标 (前端显示的徽章图片链接)
     */
    private String iconUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
