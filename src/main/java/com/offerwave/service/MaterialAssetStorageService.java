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
        dto.setContentType(canonicalContentType(extension));
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

        String detectedType = detectImageType(file);
        if (!extensionMatchesDetectedType(extension, detectedType)) {
            throw new IllegalArgumentException("图片内容与文件扩展名不匹配");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
            String expectedContentType = canonicalContentType(extension);
            boolean jpegAlias = ("jpg".equals(extension) || "jpeg".equals(extension))
                    && "image/jpg".equals(normalizedContentType);
            if (!expectedContentType.equals(normalizedContentType) && !jpegAlias) {
                throw new IllegalArgumentException("图片内容与声明的媒体类型不匹配");
            }
        }
    }

    private String detectImageType(MultipartFile file) {
        final byte[] header;
        try (var inputStream = file.getInputStream()) {
            header = inputStream.readNBytes(12);
        } catch (IOException ex) {
            throw new IllegalStateException("无法读取图片内容", ex);
        }

        if (startsWith(header, new int[]{0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) {
            return "png";
        }
        if (startsWith(header, new int[]{0xff, 0xd8, 0xff})) {
            return "jpg";
        }
        if (startsWith(header, new int[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61})
                || startsWith(header, new int[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61})) {
            return "gif";
        }
        if (header.length >= 12
                && startsWith(header, new int[]{0x52, 0x49, 0x46, 0x46})
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50) {
            return "webp";
        }
        throw new IllegalArgumentException("图片内容不是受支持的图片格式");
    }

    private boolean startsWith(byte[] value, int[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (Byte.toUnsignedInt(value[i]) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean extensionMatchesDetectedType(String extension, String detectedType) {
        if (("jpg".equals(extension) || "jpeg".equals(extension)) && "jpg".equals(detectedType)) {
            return true;
        }
        return extension.equals(detectedType);
    }

    private String canonicalContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> throw new IllegalArgumentException("不支持的图片格式");
        };
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
