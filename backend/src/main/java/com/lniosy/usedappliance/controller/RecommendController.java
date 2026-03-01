package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.product.ProductDto;
import com.lniosy.usedappliance.service.RecommendService;
import com.lniosy.usedappliance.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {
    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/home")
    public ApiResponse<List<ProductDto>> home() {
        return ApiResponse.ok(recommendService.home(SecurityUtils.currentUserId()));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<List<ProductDto>> related(@PathVariable Long productId) {
        return ApiResponse.ok(recommendService.related(productId));
    }
}
