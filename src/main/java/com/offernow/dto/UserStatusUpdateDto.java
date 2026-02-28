package com.offernow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserStatusUpdateDto implements Serializable {

    /** 1-正常, 0-封禁 */
    @NotNull
    private Integer accountStatus;
}
