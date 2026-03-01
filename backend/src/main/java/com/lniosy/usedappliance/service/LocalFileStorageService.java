package com.lniosy.usedappliance.service;

import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.file.ImageUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService {
    private final Path root;

    public LocalFileStorageService(@Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + this.root, e);
        }
    }

    public ImageUploadResponse storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException(400, "仅支持图片上传");
        }
        if (file.getSize() > 5 * 1024 * 1024L) {
            throw new BizException(400, "图片大小不能超过5MB");
        }
        String ext = extension(file.getOriginalFilename());
        if (ext.isBlank()) {
            ext = ".png";
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = root.resolve(filename);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException(500, "图片保存失败");
        }
        return new ImageUploadResponse("/uploads/" + filename, filename, file.getSize());
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        String ext = name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]{2,5}") ? ext : "";
    }
}
