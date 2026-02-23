package com.offernow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户-职位交互状态表 (收藏与追踪)
 */
@TableName(value ="user_job_status")
@Data
public class UserJobStatus implements Serializable {
    /**
     * 主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户 ID (外键)
     */
    private Long userId;

    /**
     * 关联职位 ID (外键)
     */
    private Long jobId;

    /**
     * 是否收藏 (TRUE: 是, FALSE: 否)
     */
    private Boolean isCollected;

    /**
     * 投递状态 (0:未投, 1:已投, 2:笔试, 3:面试, 4:录用, 5:感谢信)
     */
    private Integer deliveryStatus;

    /**
     * 用户个人备注
     */
    private String userNote;

    /**
     * 状态最后变更时间
     */
    private LocalDateTime updatedAt;

    /**
     * 首次关联时间
     */
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
