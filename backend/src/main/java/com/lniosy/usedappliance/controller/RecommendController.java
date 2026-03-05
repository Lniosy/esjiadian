package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.product.ProductDto;
import com.lniosy.usedappliance.dto.recommend.RecommendEventRequest;
import com.lniosy.usedappliance.service.RecommendService;
import com.lniosy.usedappliance.service.RecommendTraceService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {
    private final RecommendService recommendService;
    private final RecommendTraceService recommendTraceService;

    public RecommendController(RecommendService recommendService,
                               RecommendTraceService recommendTraceService) {
        this.recommendService = recommendService;
        this.recommendTraceService = recommendTraceService;
    }

    @GetMapping("/home")
    public ApiResponse<List<ProductDto>> home() {
        return ApiResponse.ok(recommendService.home(SecurityUtils.currentUserId()));
    }

    @GetMapping("/featured")
    public ApiResponse<List<ProductDto>> featured() {
        return ApiResponse.ok(recommendService.featured());
    }

    @GetMapping("/nearby")
    public ApiResponse<List<ProductDto>> nearby() {
        return ApiResponse.ok(recommendService.nearby(SecurityUtils.currentUserId()));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<List<ProductDto>> related(@PathVariable Long productId) {
        return ApiResponse.ok(recommendService.related(productId));
    }

    @PostMapping("/events")
    public ApiResponse<Void> recordEvent(@RequestBody @Valid RecommendEventRequest request) {
        recommendTraceService.recordEvent(
                SecurityUtils.currentUserId(),
                request.productId(),
                request.categoryId(),
                request.eventType(),
                request.eventScore() == null ? 0D : request.eventScore()
        );
        return ApiResponse.ok("记录成功", null);
    }
}
