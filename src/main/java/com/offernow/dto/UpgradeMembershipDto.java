package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 模拟购买/升级会员的数据传输对象 (DTO)
 */
@Data
@Schema(description = "升级会员请求的数据模型")
public class UpgradeMembershipDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目标等级 ID (如 2 代表 VIP)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetLevelId;
}
