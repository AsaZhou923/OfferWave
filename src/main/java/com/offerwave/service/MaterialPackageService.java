package com.offerwave.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.dto.MaterialCategorySectionDto;
import com.offerwave.dto.MaterialCategoryUpsertDto;
import com.offerwave.dto.MaterialDownloadItemDto;
import com.offerwave.dto.MaterialPackageCardDto;
import com.offerwave.dto.MaterialPackageDetailDto;
import com.offerwave.dto.MaterialPackageUpsertDto;
import com.offerwave.entity.MaterialCategory;
import com.offerwave.entity.User;

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
