package com.offerwave.service;

import com.offerwave.config.StorageProperties;
import com.offerwave.dto.MaterialImageUploadDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MaterialAssetStorageService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Autowired
    private StorageProperties storageProperties;

    public List<MaterialImageUploadDto> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("请至少上传一张图片");
        }

        List<MaterialImageUploadDto> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            uploaded.add(storeImage(file));
        }

        if (uploaded.isEmpty()) {
            throw new IllegalArgumentException("请至少上传一张图片");
        }
        return uploaded;
    }

    private MaterialImageUploadDto storeImage(MultipartFile file) {
        validateImage(file);

        LocalDate today = LocalDate.now();
        Path rootPath = storageProperties.resolveRootPath();
        Path relativeDir = Path.of(
                "materials",
                "images",
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth())
        );
        Path targetDir = rootPath.resolve(relativeDir).normalize();
        String extension = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetFile = targetDir.resolve(fileName).normalize();

        if (!targetFile.startsWith(rootPath)) {
            throw new IllegalArgumentException("图片存储路径不合法");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException ex) {
            throw new IllegalStateException("图片保存失败: " + ex.getMessage(), ex);
        }

        MaterialImageUploadDto dto = new MaterialImageUploadDto();
        dto.setOriginalName(file.getOriginalFilename());
        dto.setFileName(fileName);
        dto.setContentType(file.getContentType());
        dto.setSize(file.getSize());
        dto.setUrl(buildPublicUrl(rootPath.relativize(targetFile)));
        return dto;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }
        if (file.getSize() > storageProperties.getMaterialImageMaxSize()) {
            throw new IllegalArgumentException("图片大小不能超过 " + storageProperties.getMaterialImageMaxSize() / 1024 / 1024 + "MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!StringUtils.hasText(extension) || !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp、gif 格式图片");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("上传文件必须是图片");
        }
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String buildPublicUrl(Path relativePath) {
        return storageProperties.resolvePublicUrlPrefix() + "/" + relativePath.toString().replace('\\', '/');
    }
}
