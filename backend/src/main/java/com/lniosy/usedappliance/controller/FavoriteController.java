package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.service.FavoriteService;
import com.lniosy.usedappliance.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/ids")
    public ApiResponse<List<Long>> listIds() {
        return ApiResponse.ok(favoriteService.listIds(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{productId}/exists")
    public ApiResponse<Boolean> exists(@PathVariable Long productId) {
        return ApiResponse.ok(favoriteService.exists(SecurityUtils.currentUserId(), productId));
    }

    @PostMapping("/{productId}")
    public ApiResponse<Void> add(@PathVariable Long productId) {
        favoriteService.add(SecurityUtils.currentUserId(), productId);
        return ApiResponse.ok("收藏成功", null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@PathVariable Long productId) {
        favoriteService.remove(SecurityUtils.currentUserId(), productId);
        return ApiResponse.ok("取消收藏成功", null);
    }
}
