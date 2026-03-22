package com.offerwave.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.common.R;
import com.offerwave.dto.MaterialCategorySectionDto;
import com.offerwave.dto.MaterialDownloadItemDto;
import com.offerwave.dto.MaterialPackageCardDto;
import com.offerwave.dto.MaterialPackageDetailDto;
import com.offerwave.entity.User;
import com.offerwave.service.MaterialPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "资料包模块", description = "资料包分组、详情与会员下载能力")
public class MaterialPackageController {

    @Autowired
    private MaterialPackageService materialPackageService;

    @GetMapping("/material-categories/sections")
    @Operation(summary = "获取资料包首页分组")
    public R<List<MaterialCategorySectionDto>> listSections() {
        return R.success(materialPackageService.listPublishedSections());
    }

    @GetMapping("/material-packages")
    @Operation(summary = "分页获取资料包列表")
    public R<Page<MaterialPackageCardDto>> listPackages(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "关键词（标题/副标题）") @RequestParam(required = false) String keyword) {
        return R.success(materialPackageService.listPublishedPackages(new Page<>(page, size), categoryId, keyword));
    }

    @GetMapping("/material-packages/{id}")
    @Operation(summary = "获取资料包详情")
    public R<MaterialPackageDetailDto> getPackageDetail(
            @Parameter(description = "资料包ID") @PathVariable Long id) {
        return R.success(materialPackageService.getPublishedPackageDetail(id, getCurrentUser()));
    }

    @GetMapping("/user/material-packages/{id}/downloads")
    @Operation(summary = "获取资料包下载链接")
    @SecurityRequirement(name = "Authorization")
    public R<List<MaterialDownloadItemDto>> getDownloads(
            @Parameter(description = "资料包ID") @PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return R.error(401, "用户未登录或认证信息无效");
        }
        return R.success(materialPackageService.getPackageDownloads(id, currentUser));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
