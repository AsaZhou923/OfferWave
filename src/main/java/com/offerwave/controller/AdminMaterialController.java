package com.offerwave.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.common.R;
import com.offerwave.dto.MaterialCategoryUpsertDto;
import com.offerwave.dto.MaterialImageUploadDto;
import com.offerwave.dto.MaterialPackageCardDto;
import com.offerwave.dto.MaterialPackageDetailDto;
import com.offerwave.dto.MaterialPackageUpsertDto;
import com.offerwave.entity.MaterialCategory;
import com.offerwave.service.MaterialAssetStorageService;
import com.offerwave.service.MaterialPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "管理员资料包管理", description = "资料分类与资料包后台配置")
@SecurityRequirement(name = "Authorization")
public class AdminMaterialController {

    @Autowired
    private MaterialPackageService materialPackageService;

    @Autowired
    private MaterialAssetStorageService materialAssetStorageService;

    @GetMapping("/material-categories")
    @Operation(summary = "获取资料分类列表")
    public R<List<MaterialCategory>> listCategories() {
        return R.success(materialPackageService.listAdminCategories());
    }

    @PostMapping("/material-categories")
    @Operation(summary = "新增资料分类")
    public R<MaterialCategory> createCategory(@Valid @RequestBody MaterialCategoryUpsertDto dto) {
        return R.success(materialPackageService.saveOrUpdateCategory(null, dto));
    }

    @PutMapping("/material-categories/{id}")
    @Operation(summary = "编辑资料分类")
    public R<MaterialCategory> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Valid @RequestBody MaterialCategoryUpsertDto dto) {
        return R.success(materialPackageService.saveOrUpdateCategory(id, dto));
    }

    @GetMapping("/material-packages")
    @Operation(summary = "分页获取资料包列表")
    public R<Page<MaterialPackageCardDto>> listPackages(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "状态：0草稿/1发布/2下线") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键词（标题/副标题）") @RequestParam(required = false) String keyword) {
        return R.success(materialPackageService.listAdminPackages(new Page<>(page, size), categoryId, status, keyword));
    }

    @GetMapping("/material-packages/{id}")
    @Operation(summary = "获取资料包详情")
    public R<MaterialPackageDetailDto> getPackageDetail(
            @Parameter(description = "资料包ID") @PathVariable Long id) {
        return R.success(materialPackageService.getAdminPackageDetail(id));
    }

    @PostMapping("/material-packages")
    @Operation(summary = "新增资料包")
    public R<MaterialPackageDetailDto> createPackage(@Valid @RequestBody MaterialPackageUpsertDto dto) {
        return R.success(materialPackageService.saveOrUpdatePackage(null, dto));
    }

    @PutMapping("/material-packages/{id}")
    @Operation(summary = "编辑资料包")
    public R<MaterialPackageDetailDto> updatePackage(
            @Parameter(description = "资料包ID") @PathVariable Long id,
            @Valid @RequestBody MaterialPackageUpsertDto dto) {
        return R.success(materialPackageService.saveOrUpdatePackage(id, dto));
    }

    @PostMapping(value = "/material-packages/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传资料包图片")
    public R<List<MaterialImageUploadDto>> uploadPackageImages(
            @Parameter(description = "图片文件，支持一次上传多张") @RequestParam(value = "files", required = false) MultipartFile[] files,
            @Parameter(description = "单张图片文件") @RequestParam(value = "file", required = false) MultipartFile file) {
        List<MultipartFile> uploadFiles = new ArrayList<>();
        if (files != null && files.length > 0) {
            uploadFiles.addAll(Arrays.asList(files));
        }
        if (file != null) {
            uploadFiles.add(file);
        }
        return R.success(materialAssetStorageService.uploadImages(uploadFiles));
    }
}
