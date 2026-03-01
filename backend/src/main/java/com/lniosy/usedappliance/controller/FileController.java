package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.file.ImageUploadResponse;
import com.lniosy.usedappliance.service.LocalFileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final LocalFileStorageService localFileStorageService;

    public FileController(LocalFileStorageService localFileStorageService) {
        this.localFileStorageService = localFileStorageService;
    }

    @PostMapping("/upload/image")
    public ApiResponse<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(localFileStorageService.storeImage(file));
    }
}
