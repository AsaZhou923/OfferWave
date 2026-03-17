package com.offernow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.common.R;
import com.offernow.dto.AdminJobUpsertDto;
import com.offernow.dto.BatchCompanyStageUpdateDto;
import com.offernow.dto.JobAuditBatchDto;
import com.offernow.dto.SensitiveWordUpsertDto;
import com.offernow.dto.SystemConfigUpsertDto;
import com.offernow.dto.UserBenefitAdjustDto;
import com.offernow.dto.UserStatusUpdateDto;
import com.offernow.entity.ContentAuditLog;
import com.offernow.entity.CrawlerSyncError;
import com.offernow.entity.CrawlerSyncLog;
import com.offernow.entity.Job;
import com.offernow.entity.Membership;
import com.offernow.entity.SensitiveWord;
import com.offernow.entity.SystemConfig;
import com.offernow.entity.User;
import com.offernow.service.AdminService;
import com.offernow.service.ContentModerationService;
import com.offernow.util.JobImportParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "管理员后台")
@SecurityRequirement(name = "Authorization")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ContentModerationService contentModerationService;

    @GetMapping("/jobs/pending-audit")
    @Operation(summary = "待办审核列表")
    public R<Page<Job>> pendingAuditJobs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.success(adminService.listPendingAuditJobs(new Page<>(page, size)));
    }

    @PostMapping("/jobs/audit")
    @Operation(summary = "批量审核职位")
    public R<String> auditJobs(@Parameter(description = "批量审核参数") @Valid @RequestBody JobAuditBatchDto dto) {
        if (dto.getAuditStatus() == null || (dto.getAuditStatus() != 1 && dto.getAuditStatus() != 2)) {
            return R.error(400, "auditStatus 仅支持 1(通过) 或 2(驳回)");
        }
        return adminService.batchAuditJobs(dto.getJobIds(), dto.getAuditStatus())
                ? R.success("操作成功")
                : R.error(400, "未更新任何记录");
    }

    @DeleteMapping("/jobs")
    @Operation(summary = "批量删除职位")
    public R<String> deleteJobs(@Parameter(description = "待删除职位ID列表") @RequestBody List<Long> jobIds) {
        return adminService.batchDeleteJobs(jobIds) ? R.success("删除成功") : R.error(400, "删除失败");
    }

    @GetMapping("/jobs")
    @Operation(summary = "职位信息库（全量）")
    public R<Page<Job>> listAllJobs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键词（公司/岗位）") @RequestParam(required = false) String keyword,
            @Parameter(description = "公司名称（精确或模糊）") @RequestParam(required = false) String companyName,
            @Parameter(description = "审核状态：0待审/1上线/2驳回") @RequestParam(required = false) Integer auditStatus) {
        return R.success(adminService.listAllJobs(new Page<>(page, size), keyword, companyName, auditStatus));
    }

    @PostMapping("/jobs")
    @Operation(summary = "单条录入职位")
    public R<Job> createJob(@Parameter(description = "职位录入参数") @Valid @RequestBody AdminJobUpsertDto dto) {
        dto.setId(null);
        return R.success(adminService.saveOrUpdateJob(dto));
    }

    @PostMapping("/jobs/batch")
    @Operation(summary = "批量录入职位（JSON）")
    public R<Map<String, Integer>> createJobs(
            @Parameter(description = "批量职位录入参数") @RequestBody List<AdminJobUpsertDto> jobs) {
        int inserted = adminService.batchCreateJobs(jobs);
        return R.success(Map.of("inserted_count", inserted));
    }

    @PostMapping("/jobs/import-file")
    @Operation(summary = "Excel/CSV 批量导入职位")
    public R<Map<String, Integer>> importJobs(
            @Parameter(description = "Excel 或 CSV 文件（.xlsx/.xls/.csv）") @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.error(400, "文件不能为空");
        }
        try {
            List<AdminJobUpsertDto> jobs = JobImportParser.parse(file);
            int inserted = adminService.batchCreateJobs(jobs);
            return R.success(Map.of("received_count", jobs.size(), "inserted_count", inserted));
        } catch (IllegalArgumentException ex) {
            return R.error(400, ex.getMessage());
        } catch (IOException ex) {
            return R.error(400, "文件解析失败: " + ex.getMessage());
        }
    }

    @PutMapping("/jobs/{id}")
    @Operation(summary = "编辑职位")
    public R<Job> updateJob(
            @Parameter(description = "职位ID") @PathVariable Long id,
            @Parameter(description = "职位更新参数") @Valid @RequestBody AdminJobUpsertDto dto) {
        dto.setId(id);
        return R.success(adminService.saveOrUpdateJob(dto));
    }

    @PostMapping("/jobs/company-stage")
    @Operation(summary = "批量更新公司招聘进度")
    public R<Map<String, Integer>> updateCompanyStage(
            @Parameter(description = "公司阶段更新参数") @Valid @RequestBody BatchCompanyStageUpdateDto dto) {
        int updated = adminService.batchUpdateCompanyProcessStage(dto.getCompanyName(), dto.getProcessStage());
        return R.success(Map.of("updated_count", updated));
    }

    @PostMapping("/jobs/cleanup-expired")
    @Operation(summary = "过期职位下架")
    public R<Map<String, Integer>> cleanupExpired() {
        int updated = adminService.cleanupExpiredJobs();
        return R.success(Map.of("offline_count", updated));
    }

    @GetMapping("/crawler/sync-logs")
    @Operation(summary = "同步日志看板")
    public R<Page<CrawlerSyncLog>> syncLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.success(adminService.listCrawlerSyncLogs(new Page<>(page, size)));
    }

    @GetMapping("/crawler/error-items")
    @Operation(summary = "异常拦截列表")
    public R<Page<CrawlerSyncError>> syncErrors(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.success(adminService.listCrawlerSyncErrors(new Page<>(page, size)));
    }

    @GetMapping("/users")
    @Operation(summary = "用户列表")
    public R<Page<User>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键词（用户名/邮箱）") @RequestParam(required = false) String keyword,
            @Parameter(description = "账号状态：1正常/0封禁") @RequestParam(required = false) Integer accountStatus) {
        return R.success(adminService.listUsers(new Page<>(page, size), keyword, accountStatus));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "用户封禁/解封")
    public R<String> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户状态更新参数") @Valid @RequestBody UserStatusUpdateDto dto) {
        return adminService.updateUserStatus(id, dto.getAccountStatus()) ? R.success("更新成功") : R.error(400, "更新失败");
    }

    @PutMapping("/users/{id}/benefits")
    @Operation(summary = "权益手工发放")
    public R<String> adjustBenefits(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户权益调整参数") @RequestBody UserBenefitAdjustDto dto) {
        return adminService.adjustUserBenefits(id, dto.getMembershipId(), dto.getCustomTrackLimit())
                ? R.success("更新成功")
                : R.error(400, "更新失败");
    }

    @GetMapping("/moderation/sensitive-words")
    @Operation(summary = "敏感词列表")
    public R<Page<SensitiveWord>> listSensitiveWords(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.success(contentModerationService.listSensitiveWords(new Page<>(page, size)));
    }

    @PostMapping("/moderation/sensitive-words")
    @Operation(summary = "新增敏感词")
    public R<SensitiveWord> addSensitiveWord(
            @Parameter(description = "敏感词参数") @Valid @RequestBody SensitiveWordUpsertDto dto) {
        return R.success(contentModerationService.addSensitiveWord(dto.getWord(), dto.getEnabled()));
    }

    @PutMapping("/moderation/sensitive-words/{id}/status")
    @Operation(summary = "修改敏感词状态")
    public R<String> updateSensitiveWordStatus(
            @Parameter(description = "敏感词ID") @PathVariable Long id,
            @Parameter(description = "敏感词状态参数") @RequestBody SensitiveWordUpsertDto dto) {
        return contentModerationService.updateSensitiveWordStatus(id, dto.getEnabled())
                ? R.success("更新成功")
                : R.error(400, "更新失败");
    }

    @DeleteMapping("/moderation/sensitive-words/{id}")
    @Operation(summary = "删除敏感词")
    public R<String> deleteSensitiveWord(@Parameter(description = "敏感词ID") @PathVariable Long id) {
        return contentModerationService.deleteSensitiveWord(id) ? R.success("删除成功") : R.error(400, "删除失败");
    }

    @GetMapping("/moderation/audit-logs")
    @Operation(summary = "内容审核日志")
    public R<Page<ContentAuditLog>> listAuditLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.success(contentModerationService.listAuditLogs(new Page<>(page, size)));
    }

    @GetMapping("/memberships")
    @Operation(summary = "会员等级管理列表")
    public R<List<Membership>> listMemberships() {
        return R.success(adminService.listMemberships());
    }

    @PutMapping("/memberships/{id}")
    @Operation(summary = "更新会员等级配置")
    public R<String> updateMembership(
            @Parameter(description = "会员等级ID") @PathVariable Integer id,
            @Parameter(description = "会员配置参数") @RequestBody Membership membership) {
        membership.setId(id);
        return adminService.updateMembership(membership) ? R.success("更新成功") : R.error(400, "更新失败");
    }

    @GetMapping("/configs")
    @Operation(summary = "系统配置列表")
    public R<Page<SystemConfig>> listConfigs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "配置键（可选过滤）") @RequestParam(required = false) String configKey) {
        return R.success(adminService.listSystemConfigs(new Page<>(page, size), configKey));
    }

    @PostMapping("/configs")
    @Operation(summary = "新增/更新系统配置")
    public R<String> upsertConfig(
            @Parameter(description = "系统配置参数") @Valid @RequestBody SystemConfigUpsertDto dto) {
        return adminService.upsertSystemConfig(dto.getConfigKey(), dto.getConfigValue(), dto.getDescription())
                ? R.success("保存成功")
                : R.error(400, "保存失败");
    }
}
