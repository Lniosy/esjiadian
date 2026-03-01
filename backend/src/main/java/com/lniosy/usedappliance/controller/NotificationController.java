package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.notification.NotificationDto;
import com.lniosy.usedappliance.service.NotificationService;
import com.lniosy.usedappliance.service.SmsMockService;
import com.lniosy.usedappliance.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final SmsMockService smsMockService;

    public NotificationController(NotificationService notificationService, SmsMockService smsMockService) {
        this.notificationService = notificationService;
        this.smsMockService = smsMockService;
    }

    @GetMapping
    public ApiResponse<List<NotificationDto>> list() {
        return ApiResponse.ok(notificationService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok("已读", null);
    }

    @GetMapping("/sms-logs")
    public ApiResponse<?> smsLogs(@RequestParam(defaultValue = "30") Integer limit) {
        return ApiResponse.ok(smsMockService.listByUser(SecurityUtils.currentUserId(), limit));
    }
}
