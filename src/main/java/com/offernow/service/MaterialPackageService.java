package com.offernow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.dto.MaterialCategorySectionDto;
import com.offernow.dto.MaterialCategoryUpsertDto;
import com.offernow.dto.MaterialDownloadItemDto;
import com.offernow.dto.MaterialPackageCardDto;
import com.offernow.dto.MaterialPackageDetailDto;
import com.offernow.dto.MaterialPackageUpsertDto;
import com.offernow.entity.MaterialCategory;
import com.offernow.entity.User;

import java.util.List;

public interface MaterialPackageService {

    List<MaterialCategorySectionDto> listPublishedSections();

    Page<MaterialPackageCardDto> listPublishedPackages(Page<?> page, Long categoryId, String keyword);

    MaterialPackageDetailDto getPublishedPackageDetail(Long id, User currentUser);

    List<MaterialDownloadItemDto> getPackageDownloads(Long id, User currentUser);

    List<MaterialCategory> listAdminCategories();

    MaterialCategory saveOrUpdateCategory(Long id, MaterialCategoryUpsertDto dto);

    Page<MaterialPackageCardDto> listAdminPackages(Page<?> page, Long categoryId, Integer status, String keyword);

    MaterialPackageDetailDto getAdminPackageDetail(Long id);

    MaterialPackageDetailDto saveOrUpdatePackage(Long id, MaterialPackageUpsertDto dto);
}
