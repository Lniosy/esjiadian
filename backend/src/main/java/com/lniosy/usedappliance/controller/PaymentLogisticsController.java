package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.logistics.LogisticsDto;
import com.lniosy.usedappliance.dto.payment.PaymentCreateResponse;
import com.lniosy.usedappliance.service.LogisticsService;
import com.lniosy.usedappliance.service.PaymentService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentLogisticsController {
    private final PaymentService paymentService;
    private final LogisticsService logisticsService;

    public PaymentLogisticsController(PaymentService paymentService, LogisticsService logisticsService) {
        this.paymentService = paymentService;
        this.logisticsService = logisticsService;
    }

    @PostMapping("/payments/{orderId}/alipay")
    public ApiResponse<PaymentCreateResponse> alipay(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.create(orderId, "alipay"));
    }

    @PostMapping("/payments/{orderId}/wechat")
    public ApiResponse<PaymentCreateResponse> wechat(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.create(orderId, "wechat"));
    }

    @PostMapping("/payments/callback/{channel}")
    public ApiResponse<Void> callback(@PathVariable String channel,
                                      @RequestParam Long orderId,
                                      @RequestParam String outTradeNo,
                                      @RequestParam(defaultValue = "true") boolean success) {
        paymentService.callback(channel, orderId, outTradeNo, success);
        return ApiResponse.ok("回调处理成功", null);
    }

    @PostMapping("/orders/{orderId}/logistics")
    public ApiResponse<LogisticsDto> saveLogistics(@PathVariable Long orderId,
                                                   @RequestParam @NotBlank String companyCode,
                                                   @RequestParam @NotBlank String trackingNo,
                                                   @RequestParam(required = false) String shipNote) {
        return ApiResponse.ok(logisticsService.save(orderId, companyCode, trackingNo, shipNote));
    }

    @GetMapping("/orders/{orderId}/logistics/tracks")
    public ApiResponse<LogisticsDto> tracks(@PathVariable Long orderId) {
        return ApiResponse.ok(logisticsService.tracks(orderId));
    }
}
