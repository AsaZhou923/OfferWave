package com.offerwave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-职位状态表实体（收藏/投递）。
 */
@TableName(value = "user_job_status")
@Data
public class UserJobStatus implements Serializable {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 职位 ID */
    private Long jobId;

    /** 是否收藏 */
    private Boolean isCollected;

    /** 投递状态（0-5） */
    private Integer deliveryStatus;

    /** 用户备注 */
    private String userNote;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
