package com.offerwave.dto;

import com.offerwave.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Safe administrator-facing user projection. Authentication credentials and
 * external identity identifiers are intentionally excluded.
 */
@Data
@Schema(description = "管理员用户列表响应")
public class AdminUserResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;
    private Integer role;
    private Integer accountStatus;
    private String nickname;
    private Integer membershipId;
    private LocalDateTime membershipExpireAt;
    private String prefIndustry;
    private String prefCity;
    private String prefJob;
    private String educationBackground;
    private Integer salary;
    private Integer customTrackLimit;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public static AdminUserResponseDto from(User user) {
        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setAccountStatus(user.getAccountStatus());
        dto.setNickname(user.getNickname());
        dto.setMembershipId(user.getMembershipId());
        dto.setMembershipExpireAt(user.getMembershipExpireAt());
        dto.setPrefIndustry(user.getPrefIndustry());
        dto.setPrefCity(user.getPrefCity());
        dto.setPrefJob(user.getPrefJob());
        dto.setEducationBackground(user.getEducationBackground());
        dto.setSalary(user.getSalary());
        dto.setCustomTrackLimit(user.getCustomTrackLimit());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
