package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.evaluation.EvaluationDto;
import com.lniosy.usedappliance.dto.evaluation.EvaluationRequest;
import com.lniosy.usedappliance.dto.evaluation.RatingDetailDto;
import com.lniosy.usedappliance.service.EvaluationService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/orders/{orderId}/evaluations")
    public ApiResponse<EvaluationDto> create(@PathVariable Long orderId, @RequestBody @Valid EvaluationRequest req) {
        return ApiResponse.ok(evaluationService.create(SecurityUtils.currentUserId(), orderId, req));
    }

    @GetMapping("/products/{productId}/evaluations")
    public ApiResponse<List<EvaluationDto>> byProduct(@PathVariable Long productId) {
        return ApiResponse.ok(evaluationService.byProduct(productId));
    }

    @GetMapping("/shops/{shopId}/rating")
    public ApiResponse<Map<String, Double>> rating(@PathVariable Long shopId) {
        return ApiResponse.ok(Map.of("rating", evaluationService.shopRating(shopId)));
    }

    @GetMapping("/shops/{shopId}/rating-detail")
    public ApiResponse<RatingDetailDto> ratingDetail(@PathVariable Long shopId) {
        return ApiResponse.ok(evaluationService.shopRatingDetail(shopId));
    }
}
