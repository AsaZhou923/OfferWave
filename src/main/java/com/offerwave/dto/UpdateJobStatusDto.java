package com.offerwave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新职位状态请求 DTO。
 */
@Data
@Schema(description = "更新职位状态的数据模型")
public class UpdateJobStatusDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否收藏")
    private Boolean isCollected;

    @Schema(description = "投递状态码（见接口文档枚举）")
    @Min(value = 0, message = "投递状态码必须在 0 到 5 之间")
    @Max(value = 5, message = "投递状态码必须在 0 到 5 之间")
    private Integer deliveryStatus;

    @Schema(description = "用户备注（最多 200 字）")
    @Size(max = 200, message = "用户备注最多 200 字")
    private String userNote;
}
