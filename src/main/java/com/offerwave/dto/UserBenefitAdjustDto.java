package com.offerwave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "管理员手工调整用户权益请求参数")
public class UserBenefitAdjustDto implements Serializable {

    @Schema(description = "会员等级 ID（不传表示不调整会员等级）")
    private Integer membershipId;

    @Schema(description = "自定义追踪上限（null 表示清空手工配置并恢复会员默认权益）")
    private Integer customTrackLimit;
}
