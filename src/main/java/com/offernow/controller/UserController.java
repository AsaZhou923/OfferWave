package com.offernow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.common.R;
import com.offernow.dto.MyJobDto;
import com.offernow.dto.UpdateJobStatusDto;
import com.offernow.dto.UserPreferenceDto;
import com.offernow.dto.UpgradeMembershipDto;
import com.offernow.entity.User;
import com.offernow.service.TrackingService;
import com.offernow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户个人中心控制器。
 */
@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "用户个人中心", description = "管理用户个人信息、偏好、会员状态和职位追踪")
@SecurityRequirement(name = "Authorization")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TrackingService trackingService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    private R<Object> handleUnauthenticatedUser() {
        return R.error(401, "用户未登录或认证信息无效");
    }

    @GetMapping("/me")
    @Operation(summary = "获取个人信息", description = "返回当前登录用户的个人信息、偏好和会员信息")
    public R<?> getMyInfo() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return handleUnauthenticatedUser();
        }
        Map<String, Object> userInfo = userService.getUserInfo(currentUser.getId());
        return R.success(userInfo);
    }

    @PutMapping("/preferences")
    @Operation(summary = "更新求职偏好", description = "更新用户求职偏好设置")
    public R<?> updateMyPreferences(
            @Parameter(description = "求职偏好参数") @RequestBody UserPreferenceDto preferenceDto) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return handleUnauthenticatedUser();
        }
        boolean success = userService.updatePreferences(currentUser.getId(), preferenceDto);
        if (success) {
            return R.success("偏好设置已更新");
        } else {
            return R.error("更新失败");
        }
    }

    @PostMapping("/membership/upgrade")
    @Operation(summary = "模拟升级会员", description = "开发测试接口：将当前用户升级到目标会员等级")
    public R<?> upgradeMyMembership(
            @Parameter(description = "升级会员参数") @RequestBody UpgradeMembershipDto upgradeDto) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return handleUnauthenticatedUser();
        }
        if (upgradeDto == null || upgradeDto.getTargetLevelId() == null) {
            return R.error(400, "目标等级ID不能为空");
        }
        Map<String, Object> result = userService.upgradeMembership(currentUser.getId(), upgradeDto.getTargetLevelId());
        return R.success(result);
    }

    @PostMapping("/jobs/{job_id}/status")
    @Operation(summary = "更新职位状态", description = "更新职位收藏/投递状态")
    public R<?> updateJobStatus(
            @Parameter(description = "职位ID") @PathVariable("job_id") Long jobId,
            @Parameter(description = "职位状态参数") @RequestBody UpdateJobStatusDto statusDto) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return handleUnauthenticatedUser();
        }
        trackingService.updateJobStatus(currentUser.getId(), jobId, statusDto);
        return R.success("状态已更新");
    }

    @GetMapping("/my-jobs")
    @Operation(summary = "获取我的职位列表", description = "获取用户“收藏夹”或“投递进度表”的数据")
    public R<?> getMyJobs(
            @Parameter(description = "列表类型：collected=仅收藏，delivered=已投递") @RequestParam String type,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return handleUnauthenticatedUser();
        }

        Page<MyJobDto> pageInfo = new Page<>(page, size);
        Page<MyJobDto> resultPage = trackingService.getMyJobs(currentUser.getId(), type, pageInfo);
        return R.success(resultPage);
    }
}
