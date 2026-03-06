package com.offernow.dto;

import com.offernow.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 职位详情响应 DTO。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "职位详情响应模型")
public class JobDetailDto extends Job {

    @Schema(description = "当前用户对该职位的个性化状态（未登录时为 null）")
    private MyStatus myStatus;

    /**
     * 当前用户对该职位的追踪状态。
     */
    @Data
    @Schema(description = "用户对职位的个人追踪状态")
    public static class MyStatus {

        @Schema(description = "是否收藏该职位")
        private boolean isCollected;

        @Schema(description = "投递状态码（0:未投递, 1:已投递, 2:笔试中, 3:面试中, 4:已录用, 5:流程结束）")
        private Integer deliveryStatus;

        @Schema(description = "投递状态文本")
        private String deliveryStatusStr;

        @Schema(description = "用户备注")
        private String userNote;
    }
}
