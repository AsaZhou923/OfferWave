package com.offerwave.service;

import com.offerwave.config.StorageProperties;
import com.offerwave.dto.MaterialImageUploadDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialAssetStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadImagesShouldStoreFilesAndReturnPublicUrl() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRootPath(tempDir.toString());
        storageProperties.setPublicUrlPrefix("/uploads");
        storageProperties.setMaterialImageMaxSize(1024 * 1024);

        MaterialAssetStorageService storageService = new MaterialAssetStorageService();
        ReflectionTestUtils.setField(storageService, "storageProperties", storageProperties);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "cover.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        List<MaterialImageUploadDto> uploaded = storageService.uploadImages(List.of(file));

        assertEquals(1, uploaded.size());
        MaterialImageUploadDto image = uploaded.get(0);
        assertEquals("cover.png", image.getOriginalName());
        assertTrue(image.getUrl().startsWith("/uploads/materials/images/"));

        Path storedFile = storageProperties.resolveRootPath()
                .resolve(image.getUrl().replaceFirst("^/uploads/", "").replace('/', File.separatorChar));
        assertTrue(Files.exists(storedFile));
    }

    @Test
    void uploadImagesShouldRejectUnsupportedExtension() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRootPath(tempDir.toString());
        storageProperties.setPublicUrlPrefix("/uploads");

        MaterialAssetStorageService storageService = new MaterialAssetStorageService();
        ReflectionTestUtils.setField(storageService, "storageProperties", storageProperties);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "cover.txt",
                "text/plain",
                "not-an-image".getBytes()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageService.uploadImages(List.of(file))
        );

        assertTrue(exception.getMessage().contains("jpg"));
    }
}
