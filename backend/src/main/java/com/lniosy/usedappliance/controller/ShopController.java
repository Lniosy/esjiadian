package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.shop.ShopDto;
import com.lniosy.usedappliance.dto.shop.ShopOverviewDto;
import com.lniosy.usedappliance.dto.shop.ShopUpsertRequest;
import com.lniosy.usedappliance.service.ShopService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops")
public class ShopController {
    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/me")
    public ApiResponse<ShopDto> myShop() {
        return ApiResponse.ok(shopService.myShop(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<ShopDto> upsertMyShop(@RequestBody @Valid ShopUpsertRequest req) {
        return ApiResponse.ok(shopService.upsertMyShop(SecurityUtils.currentUserId(), req));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShopDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(shopService.detail(id));
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<ShopDto> byUserId(@PathVariable Long userId) {
        return ApiResponse.ok(shopService.byUserId(userId));
    }

    @GetMapping("/{id}/overview")
    public ApiResponse<ShopOverviewDto> overview(@PathVariable Long id) {
        return ApiResponse.ok(shopService.overview(id));
    }
}
