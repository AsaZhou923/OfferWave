package com.offernow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.common.R;
import com.offernow.dto.UpdateJobStatusDto;
import com.offernow.dto.UserPreferenceDto;
import com.offernow.dto.UpgradeMembershipDto;
import com.offernow.entity.Job;
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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "用户个人中心", description = "管理用户个人信息、偏好、会员状态和职位追踪")
@SecurityRequirement(name = "Authorization") // 标记所有接口需要认证
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TrackingService trackingService;

    /**
     * 从 Spring Security 上下文中获取当前登录的用户信息
     * 
     * @return 如果用户已登录，返回 User 对象；否则返回 null
     */
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
    @Operation(summary = "获取个人信息 (含权益)", description = "获取当前登录用户的详细信息，包括求职偏好、会员权益详情以及统计数据")
    public R<?> getMyInfo() {
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return handleUnauthenticatedUser();
        Map<String, Object> userInfo = userService.getUserInfo(currentUser.getId());
        return R.success(userInfo);
    }

    @PutMapping("/preferences")
    @Operation(summary = "更新求职偏好", description = "更新用户的求职意向，用于首页推荐算法")
    public R<?> updateMyPreferences(@RequestBody UserPreferenceDto preferenceDto) {
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return handleUnauthenticatedUser();
        boolean success = userService.updatePreferences(currentUser.getId(), preferenceDto);
        if (success) {
            return R.success("偏好设置已更新");
        } else {
            return R.error("更新失败");
        }
    }

    @PostMapping("/membership/upgrade")
    @Operation(summary = "模拟购买/升级会员 (MVP)", description = "开发测试专用接口。将当前用户直接升级为指定等级")
    public R<?> upgradeMyMembership(@RequestBody UpgradeMembershipDto upgradeDto) {
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return handleUnauthenticatedUser();
        if (upgradeDto == null || upgradeDto.getTargetLevelId() == null) {
            return R.error(400, "目标等级ID不能为空");
        }
        Map<String, Object> result = userService.upgradeMembership(currentUser.getId(), upgradeDto.getTargetLevelId());
        return R.success(result);
    }

    @PostMapping("/jobs/{job_id}/status")
    @Operation(summary = "更新职位状态(收藏/投递)", description = "用户手动标记某个职位的状态")
    public R<?> updateJobStatus(@Parameter(description = "职位ID") @PathVariable("job_id") Long jobId,
            @RequestBody UpdateJobStatusDto statusDto) {
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return (R<?>) handleUnauthenticatedUser();
        trackingService.updateJobStatus(currentUser.getId(), jobId, statusDto);
        return R.success("状态已更新");
    }

    @GetMapping("/my-jobs")
    @Operation(summary = "获取我的职位列表", description = "获取用户“收藏夹”或“投递进度表”的数据")
    public R<?> getMyJobs(@Parameter(description = "'collected' (仅收藏) 或 'delivered' (已投递)") @RequestParam String type,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return handleUnauthenticatedUser();

        Page<Job> pageInfo = new Page<>(page, size);
        Page<Job> resultPage = trackingService.getMyJobs(currentUser.getId(), type, pageInfo);
        return R.success(resultPage);
    }
}
