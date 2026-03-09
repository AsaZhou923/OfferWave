package com.offernow.dto;

import com.offernow.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的职位列表项。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MyJobDto extends Job {

    @Schema(description = "投递状态码（0:未投递, 1:已投递, 2:笔试中, 3:面试中, 4:已录用, 5:流程结束）")
    private Integer deliveryStatus;
}
