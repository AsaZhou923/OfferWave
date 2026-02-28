package com.offernow.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserBenefitAdjustDto implements Serializable {

    private Integer membershipId;

    /** null 表示清空手工配置，恢复会员权益策略 */
    private Integer customTrackLimit;
}
