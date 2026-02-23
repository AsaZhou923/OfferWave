package com.offernow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户求职偏好的数据传输对象 (DTO)
 */
@Data
@Schema(description = "更新用户求职偏好的数据模型")
public class UserPreferenceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "偏好行业 (如 \"互联网,教育\")")
    private String prefIndustry;

    @Schema(description = "偏好城市 (如 \"上海\")")
    private String prefCity;

    @Schema(description = "偏好岗位 (如 \"Java开发\")")
    private String prefJob;
}
