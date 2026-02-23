package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "更新职位状态的数据模型")
public class UpdateJobStatusDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否收藏")
    private Boolean isCollected;

    @Schema(description = "投递状态码 (见API文档枚举)")
    private Integer deliveryStatus;

    @Schema(description = "用户备注 (限200字)")
    private String userNote;
}
