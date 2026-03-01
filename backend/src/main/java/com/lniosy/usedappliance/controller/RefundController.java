package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.refund.RefundApplyRequest;
import com.lniosy.usedappliance.dto.refund.RefundDecisionRequest;
import com.lniosy.usedappliance.dto.refund.RefundDto;
import com.lniosy.usedappliance.dto.refund.RefundReturnShipRequest;
import com.lniosy.usedappliance.service.RefundService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/refund")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/apply")
    public ApiResponse<RefundDto> apply(@PathVariable Long orderId, @RequestBody @Valid RefundApplyRequest req) {
        return ApiResponse.ok(refundService.apply(SecurityUtils.currentUserId(), orderId, req));
    }

    @GetMapping
    public ApiResponse<RefundDto> detail(@PathVariable Long orderId) {
        return ApiResponse.ok(refundService.detail(orderId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/approve")
    public ApiResponse<RefundDto> approve(@PathVariable Long orderId) {
        return ApiResponse.ok(refundService.approve(SecurityUtils.currentUserId(), orderId));
    }

    @PostMapping("/reject")
    public ApiResponse<RefundDto> reject(@PathVariable Long orderId, @RequestBody(required = false) RefundDecisionRequest req) {
        return ApiResponse.ok(refundService.reject(SecurityUtils.currentUserId(), orderId, req));
    }

    @PostMapping("/ship-return")
    public ApiResponse<RefundDto> shipReturn(@PathVariable Long orderId, @RequestBody @Valid RefundReturnShipRequest req) {
        return ApiResponse.ok(refundService.shipReturn(SecurityUtils.currentUserId(), orderId, req));
    }

    @PostMapping("/confirm-return")
    public ApiResponse<RefundDto> confirmReturn(@PathVariable Long orderId) {
        return ApiResponse.ok(refundService.confirmReturn(SecurityUtils.currentUserId(), orderId));
    }
}
