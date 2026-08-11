package com.offerwave.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.common.NotFoundException;
import com.offerwave.common.PrivilegeException;
import com.offerwave.dto.MaterialCatalogItemDto;
import com.offerwave.dto.MaterialCategorySectionDto;
import com.offerwave.dto.MaterialCategoryUpsertDto;
import com.offerwave.dto.MaterialDownloadItemDto;
import com.offerwave.dto.MaterialPackageCardDto;
import com.offerwave.dto.MaterialPackageDetailDto;
import com.offerwave.dto.MaterialPackageUpsertDto;
import com.offerwave.entity.MaterialCategory;
import com.offerwave.entity.MaterialDownload;
import com.offerwave.entity.MaterialPackage;
import com.offerwave.entity.User;
import com.offerwave.mapper.MaterialCategoryMapper;
import com.offerwave.mapper.MaterialDownloadMapper;
import com.offerwave.mapper.MaterialPackageMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MaterialPackageServiceImpl implements MaterialPackageService {

    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_ENABLED = 1;
    private static final int ACCESS_TYPE_MEMBER = 1;

    @Autowired
    private MaterialCategoryMapper materialCategoryMapper;

    @Autowired
    private MaterialPackageMapper materialPackageMapper;

    @Autowired
    private MaterialDownloadMapper materialDownloadMapper;

    @Autowired
    private MembershipAccessService membershipAccessService;

    @Override
    public List<MaterialCategorySectionDto> listPublishedSections() {
        LambdaQueryWrapper<MaterialCategory> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(MaterialCategory::getStatus, STATUS_ENABLED)
                .orderByAsc(MaterialCategory::getSortOrder)
                .orderByAsc(MaterialCategory::getId);
        List<MaterialCategory> categories = materialCategoryMapper.selectList(categoryWrapper);
        if (categories.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<MaterialPackage> packageWrapper = new LambdaQueryWrapper<>();
        packageWrapper.eq(MaterialPackage::getStatus, STATUS_PUBLISHED)
                .orderByAsc(MaterialPackage::getSortOrder)
                .orderByDesc(MaterialPackage::getPublishedAt)
                .orderByDesc(MaterialPackage::getId);
        List<MaterialPackage> packages = materialPackageMapper.selectList(packageWrapper);
        Map<Long, List<MaterialPackage>> packagesByCategoryId = packages.stream()
                .collect(Collectors.groupingBy(MaterialPackage::getCategoryId, LinkedHashMap::new, Collectors.toList()));

        List<MaterialCategorySectionDto> sections = new ArrayList<>();
        for (MaterialCategory category : categories) {
            List<MaterialPackage> currentPackages = packagesByCategoryId.get(category.getId());
            if (currentPackages == null || currentPackages.isEmpty()) {
                continue;
            }
            MaterialCategorySectionDto section = new MaterialCategorySectionDto();
            section.setId(category.getId());
            section.setName(category.getName());
            section.setSlug(category.getSlug());
            section.setDescription(category.getDescription());
            section.setSortOrder(category.getSortOrder());
            section.setPackages(currentPackages.stream()
                    .map(pkg -> toCardDto(pkg, category.getName()))
                    .collect(Collectors.toList()));
            sections.add(section);
        }
        return sections;
    }

    @Override
    public Page<MaterialPackageCardDto> listPublishedPackages(Page<?> page, Long categoryId, String keyword) {
        Page<MaterialPackage> queryPage = new Page<>(page.getCurrent(), page.getSize());
        LambdaQueryWrapper<MaterialPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialPackage::getStatus, STATUS_PUBLISHED)
                .eq(categoryId != null, MaterialPackage::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword),
                        w -> w.like(MaterialPackage::getTitle, keyword).or().like(MaterialPackage::getSubtitle, keyword))
                .orderByAsc(MaterialPackage::getSortOrder)
                .orderByDesc(MaterialPackage::getPublishedAt)
                .orderByDesc(MaterialPackage::getId);
        Page<MaterialPackage> materialPage = materialPackageMapper.selectPage(queryPage, wrapper);
        Map<Long, String> categoryNames = loadCategoryNameMap(materialPage.getRecords());

        Page<MaterialPackageCardDto> result = new Page<>(materialPage.getCurrent(), materialPage.getSize(), materialPage.getTotal());
        result.setRecords(materialPage.getRecords().stream()
                .map(pkg -> toCardDto(pkg, categoryNames.get(pkg.getCategoryId())))
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    public MaterialPackageDetailDto getPublishedPackageDetail(Long id, User currentUser) {
        MaterialPackage materialPackage = requirePackage(id);
        if (!Objects.equals(materialPackage.getStatus(), STATUS_PUBLISHED)) {
            throw new NotFoundException("资料包不存在或未发布");
        }

        incrementViewCount(materialPackage);
        MaterialCategory category = materialCategoryMapper.selectById(materialPackage.getCategoryId());
        return toDetailDto(materialPackage, category, canDownload(materialPackage, currentUser), false);
    }

    @Override
    public List<MaterialDownloadItemDto> getPackageDownloads(Long id, User currentUser) {
        MaterialPackage materialPackage = requirePackage(id);
        if (!Objects.equals(materialPackage.getStatus(), STATUS_PUBLISHED)) {
            throw new NotFoundException("资料包不存在或未发布");
        }
        if (!canDownload(materialPackage, currentUser)) {
            throw new PrivilegeException("该资料包下载仅对会员开放，请升级会员后下载");
        }

        LambdaQueryWrapper<MaterialDownload> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialDownload::getPackageId, id)
                .eq(MaterialDownload::getStatus, STATUS_ENABLED)
                .orderByAsc(MaterialDownload::getSortOrder)
                .orderByAsc(MaterialDownload::getId);
        List<MaterialDownload> downloads = materialDownloadMapper.selectList(wrapper);
        incrementDownloadCount(materialPackage);
        return downloads.stream().map(this::toDownloadDto).collect(Collectors.toList());
    }

    @Override
    public List<MaterialCategory> listAdminCategories() {
        LambdaQueryWrapper<MaterialCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MaterialCategory::getSortOrder).orderByAsc(MaterialCategory::getId);
        return materialCategoryMapper.selectList(wrapper);
    }

    @Override
    public MaterialCategory saveOrUpdateCategory(Long id, MaterialCategoryUpsertDto dto) {
        validateCategorySlug(id, dto.getSlug());
        MaterialCategory category = new MaterialCategory();
        category.setId(id);
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        if (id == null) {
            materialCategoryMapper.insert(category);
        } else if (materialCategoryMapper.updateById(category) <= 0) {
            throw new NotFoundException("资料分类不存在");
        }
        return materialCategoryMapper.selectById(category.getId());
    }

    @Override
    public Page<MaterialPackageCardDto> listAdminPackages(Page<?> page, Long categoryId, Integer status, String keyword) {
        Page<MaterialPackage> queryPage = new Page<>(page.getCurrent(), page.getSize());
        LambdaQueryWrapper<MaterialPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(categoryId != null, MaterialPackage::getCategoryId, categoryId)
                .eq(status != null, MaterialPackage::getStatus, status)
                .and(StringUtils.hasText(keyword),
                        w -> w.like(MaterialPackage::getTitle, keyword).or().like(MaterialPackage::getSubtitle, keyword))
                .orderByAsc(MaterialPackage::getSortOrder)
                .orderByDesc(MaterialPackage::getUpdatedAt)
                .orderByDesc(MaterialPackage::getId);
        Page<MaterialPackage> materialPage = materialPackageMapper.selectPage(queryPage, wrapper);
        Map<Long, String> categoryNames = loadCategoryNameMap(materialPage.getRecords());

        Page<MaterialPackageCardDto> result = new Page<>(materialPage.getCurrent(), materialPage.getSize(), materialPage.getTotal());
        result.setRecords(materialPage.getRecords().stream()
                .map(pkg -> toCardDto(pkg, categoryNames.get(pkg.getCategoryId())))
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    public MaterialPackageDetailDto getAdminPackageDetail(Long id) {
        MaterialPackage materialPackage = requirePackage(id);
        MaterialCategory category = materialCategoryMapper.selectById(materialPackage.getCategoryId());
        MaterialPackageDetailDto detailDto = toDetailDto(materialPackage, category, true, false);
        detailDto.setDownloads(loadDownloadDtos(id, null));
        return detailDto;
    }

    @Override
    @Transactional
    public MaterialPackageDetailDto saveOrUpdatePackage(Long id, MaterialPackageUpsertDto dto) {
        MaterialCategory category = materialCategoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new NotFoundException("资料分类不存在");
        }
        validatePackageSlug(id, dto.getSlug());

        MaterialPackage materialPackage = new MaterialPackage();
        materialPackage.setId(id);
        materialPackage.setCategoryId(dto.getCategoryId());
        materialPackage.setTitle(dto.getTitle());
        materialPackage.setSlug(dto.getSlug());
        materialPackage.setSubtitle(dto.getSubtitle());
        materialPackage.setIconUrl(dto.getIconUrl());
        materialPackage.setCoverImageUrl(dto.getCoverImageUrl());
        materialPackage.setExcerpt(dto.getExcerpt());
        materialPackage.setContent(dto.getContent());
        materialPackage.setPreviewImages(toJson(dto.getPreviewImages()));
        materialPackage.setFileCatalog(toJson(dto.getFileCatalog()));
        materialPackage.setDownloadTip(dto.getDownloadTip());
        materialPackage.setAccessType(dto.getAccessType() == null ? ACCESS_TYPE_MEMBER : dto.getAccessType());
        materialPackage.setStatus(dto.getStatus() == null ? STATUS_PUBLISHED : dto.getStatus());
        materialPackage.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        materialPackage.setPublishedAt(dto.getPublishedAt() == null ? LocalDateTime.now() : dto.getPublishedAt());

        if (id == null) {
            materialPackage.setViewCount(0L);
            materialPackage.setDownloadCount(0L);
            materialPackageMapper.insert(materialPackage);
        } else if (materialPackageMapper.updateById(materialPackage) <= 0) {
            throw new NotFoundException("资料包不存在");
        }

        replaceDownloads(materialPackage.getId(), dto.getDownloads());
        return getAdminPackageDetail(materialPackage.getId());
    }

    private void replaceDownloads(Long packageId, List<MaterialDownloadItemDto> downloads) {
        LambdaQueryWrapper<MaterialDownload> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(MaterialDownload::getPackageId, packageId);
        materialDownloadMapper.delete(deleteWrapper);

        if (downloads == null || downloads.isEmpty()) {
            return;
        }

        for (int i = 0; i < downloads.size(); i++) {
            MaterialDownloadItemDto dto = downloads.get(i);
            MaterialDownload download = new MaterialDownload();
            download.setPackageId(packageId);
            download.setTitle(dto.getTitle());
            download.setDownloadUrl(dto.getDownloadUrl());
            download.setExtractionCode(dto.getExtractionCode());
            download.setFileType(dto.getFileType());
            download.setFileSize(dto.getFileSize());
            download.setDescription(dto.getDescription());
            download.setSortOrder(dto.getSortOrder() == null ? i : dto.getSortOrder());
            download.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
            materialDownloadMapper.insert(download);
        }
    }

    private MaterialPackage requirePackage(Long id) {
        MaterialPackage materialPackage = materialPackageMapper.selectById(id);
        if (materialPackage == null) {
            throw new NotFoundException("资料包不存在");
        }
        return materialPackage;
    }

    private void validateCategorySlug(Long id, String slug) {
        LambdaQueryWrapper<MaterialCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialCategory::getSlug, slug);
        MaterialCategory existed = materialCategoryMapper.selectOne(wrapper);
        if (existed != null && !Objects.equals(existed.getId(), id)) {
            throw new IllegalArgumentException("分类标识已存在");
        }
    }

    private void validatePackageSlug(Long id, String slug) {
        LambdaQueryWrapper<MaterialPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialPackage::getSlug, slug);
        MaterialPackage existed = materialPackageMapper.selectOne(wrapper);
        if (existed != null && !Objects.equals(existed.getId(), id)) {
            throw new IllegalArgumentException("资料包标识已存在");
        }
    }

    private MaterialPackageCardDto toCardDto(MaterialPackage materialPackage, String categoryName) {
        MaterialPackageCardDto dto = new MaterialPackageCardDto();
        dto.setId(materialPackage.getId());
        dto.setCategoryId(materialPackage.getCategoryId());
        dto.setCategoryName(categoryName);
        dto.setTitle(materialPackage.getTitle());
        dto.setSlug(materialPackage.getSlug());
        dto.setSubtitle(materialPackage.getSubtitle());
        dto.setExcerpt(materialPackage.getExcerpt());
        dto.setIconUrl(materialPackage.getIconUrl());
        dto.setCoverImageUrl(materialPackage.getCoverImageUrl());
        dto.setAccessType(materialPackage.getAccessType());
        dto.setViewCount(materialPackage.getViewCount());
        dto.setSortOrder(materialPackage.getSortOrder());
        dto.setPublishedAt(materialPackage.getPublishedAt());
        return dto;
    }

    private MaterialPackageDetailDto toDetailDto(MaterialPackage materialPackage,
                                                 MaterialCategory category,
                                                 boolean canDownload,
                                                 boolean includeDownloads) {
        MaterialPackageDetailDto dto = new MaterialPackageDetailDto();
        dto.setId(materialPackage.getId());
        dto.setCategoryId(materialPackage.getCategoryId());
        dto.setCategoryName(category == null ? null : category.getName());
        dto.setTitle(materialPackage.getTitle());
        dto.setSlug(materialPackage.getSlug());
        dto.setSubtitle(materialPackage.getSubtitle());
        dto.setExcerpt(materialPackage.getExcerpt());
        dto.setIconUrl(materialPackage.getIconUrl());
        dto.setCoverImageUrl(materialPackage.getCoverImageUrl());
        dto.setContent(materialPackage.getContent());
        dto.setDownloadTip(materialPackage.getDownloadTip());
        dto.setAccessType(materialPackage.getAccessType());
        dto.setStatus(materialPackage.getStatus());
        dto.setSortOrder(materialPackage.getSortOrder());
        dto.setViewCount(materialPackage.getViewCount());
        dto.setDownloadCount(materialPackage.getDownloadCount());
        dto.setPublishedAt(materialPackage.getPublishedAt());
        dto.setRequiresMembership(Objects.equals(materialPackage.getAccessType(), ACCESS_TYPE_MEMBER));
        dto.setCanDownload(canDownload);
        dto.setPreviewImages(parsePreviewImages(materialPackage.getPreviewImages()));
        dto.setFileCatalog(parseCatalog(materialPackage.getFileCatalog()));
        if (includeDownloads) {
            dto.setDownloads(loadDownloadDtos(materialPackage.getId(), STATUS_ENABLED));
        }
        return dto;
    }

    private List<MaterialDownloadItemDto> loadDownloadDtos(Long packageId, Integer status) {
        LambdaQueryWrapper<MaterialDownload> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialDownload::getPackageId, packageId)
                .eq(status != null, MaterialDownload::getStatus, status)
                .orderByAsc(MaterialDownload::getSortOrder)
                .orderByAsc(MaterialDownload::getId);
        return materialDownloadMapper.selectList(wrapper).stream()
                .map(this::toDownloadDto)
                .collect(Collectors.toList());
    }

    private MaterialDownloadItemDto toDownloadDto(MaterialDownload download) {
        MaterialDownloadItemDto dto = new MaterialDownloadItemDto();
        BeanUtils.copyProperties(download, dto);
        return dto;
    }

    private Map<Long, String> loadCategoryNameMap(List<MaterialPackage> packages) {
        if (packages == null || packages.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> categoryIds = packages.stream()
                .map(MaterialPackage::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MaterialCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MaterialCategory::getId, categoryIds);
        return materialCategoryMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getName, (left, right) -> left));
    }

    private List<String> parsePreviewImages(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new ArrayList<>();
        }
        JSONArray array = JSONUtil.parseArray(raw);
        return JSONUtil.toList(array, String.class);
    }

    private List<MaterialCatalogItemDto> parseCatalog(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new ArrayList<>();
        }
        JSONArray array = JSONUtil.parseArray(raw);
        return JSONUtil.toList(array, MaterialCatalogItemDto.class);
    }

    private String toJson(Object value) {
        return JSONUtil.toJsonStr(value == null ? List.of() : value);
    }

    private boolean canDownload(MaterialPackage materialPackage, User user) {
        return !Objects.equals(materialPackage.getAccessType(), ACCESS_TYPE_MEMBER)
                || membershipAccessService.isVip(user);
    }

    private void incrementViewCount(MaterialPackage materialPackage) {
        if (materialPackageMapper.incrementViewCount(materialPackage.getId()) <= 0) {
            throw new NotFoundException("资料包不存在");
        }
        long nextCount = materialPackage.getViewCount() == null ? 1L : materialPackage.getViewCount() + 1L;
        materialPackage.setViewCount(nextCount);
    }

    private void incrementDownloadCount(MaterialPackage materialPackage) {
        if (materialPackageMapper.incrementDownloadCount(materialPackage.getId()) <= 0) {
            throw new NotFoundException("资料包不存在");
        }
        long nextCount = materialPackage.getDownloadCount() == null ? 1L : materialPackage.getDownloadCount() + 1L;
        materialPackage.setDownloadCount(nextCount);
    }
}
