package com.offerwave.service;

import com.offerwave.dto.MaterialPackageDetailDto;
import com.offerwave.entity.MaterialPackage;
import com.offerwave.mapper.MaterialCategoryMapper;
import com.offerwave.mapper.MaterialDownloadMapper;
import com.offerwave.mapper.MaterialPackageMapper;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialPackageServiceImplTest {

    @Mock
    private MaterialCategoryMapper materialCategoryMapper;

    @Mock
    private MaterialPackageMapper materialPackageMapper;

    @Mock
    private MaterialDownloadMapper materialDownloadMapper;

    @Mock
    private MembershipAccessService membershipAccessService;

    @InjectMocks
    private MaterialPackageServiceImpl materialPackageService;

    @Test
    void packageDetailShouldIncrementViewCountAtomically() {
        MaterialPackage materialPackage = publishedPublicPackage(7L);
        materialPackage.setViewCount(12L);
        when(materialPackageMapper.selectById(7L)).thenReturn(materialPackage);
        when(materialPackageMapper.incrementViewCount(7L)).thenReturn(1);

        MaterialPackageDetailDto result = materialPackageService.getPublishedPackageDetail(7L, null);

        assertEquals(13L, result.getViewCount());
        verify(materialPackageMapper).incrementViewCount(7L);
    }

    @Test
    void packageDownloadsShouldIncrementDownloadCountAtomically() {
        MaterialPackage materialPackage = publishedPublicPackage(8L);
        materialPackage.setDownloadCount(3L);
        when(materialPackageMapper.selectById(8L)).thenReturn(materialPackage);
        when(materialDownloadMapper.selectList(any())).thenReturn(List.of());
        when(materialPackageMapper.incrementDownloadCount(8L)).thenReturn(1);

        assertEquals(List.of(), materialPackageService.getPackageDownloads(8L, null));
        verify(materialPackageMapper).incrementDownloadCount(8L);
    }

    @Test
    void counterMapperMethodsShouldUseInDatabaseIncrementExpressions() throws Exception {
        Update viewUpdate = MaterialPackageMapper.class
                .getMethod("incrementViewCount", Long.class)
                .getAnnotation(Update.class);
        Update downloadUpdate = MaterialPackageMapper.class
                .getMethod("incrementDownloadCount", Long.class)
                .getAnnotation(Update.class);

        assertNotNull(viewUpdate);
        assertNotNull(downloadUpdate);
        assertTrue(String.join(" ", viewUpdate.value()).contains("view_count = COALESCE(view_count, 0) + 1"));
        assertTrue(String.join(" ", downloadUpdate.value()).contains("download_count = COALESCE(download_count, 0) + 1"));
    }

    private static MaterialPackage publishedPublicPackage(Long id) {
        MaterialPackage materialPackage = new MaterialPackage();
        materialPackage.setId(id);
        materialPackage.setStatus(1);
        materialPackage.setAccessType(0);
        materialPackage.setViewCount(0L);
        materialPackage.setDownloadCount(0L);
        return materialPackage;
    }
}
