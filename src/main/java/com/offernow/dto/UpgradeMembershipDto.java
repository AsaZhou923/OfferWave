package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 会员升级请求 DTO。
 */
@Data
@Schema(description = "升级会员请求模型")
public class UpgradeMembershipDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目标等级 ID（例如：2 表示 VIP）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetLevelId;
}
