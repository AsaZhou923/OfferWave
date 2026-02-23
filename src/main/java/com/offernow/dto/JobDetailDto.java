package com.offernow.dto;

import com.offernow.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "职位详情响应的数据模型")
public class JobDetailDto extends Job {

    @Schema(description = "当前用户的个性化状态 (未登录时为 null)")
    private MyStatus myStatus;

    @Data
    @Schema(description = "用户对职位的个人追踪状态")
    public static class MyStatus {
        @Schema(description = "是否收藏")
        private boolean isCollected;

        @Schema(description = "投递状态码 (0:未投, 1:已投, 2:笔试, 3:面试, 4:录用, 5:感谢信)")
        private Integer deliveryStatus;
        
        @Schema(description = "投递状态文本")
        private String deliveryStatusStr;

        @Schema(description = "用户个人备注")
        private String userNote;
    }
}
