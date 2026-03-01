package com.lniosy.usedappliance.dto.notification;

public record SmsNotificationLogDto(
        Long id,
        Long userId,
        String phoneMasked,
        String type,
        String title,
        String content,
        String provider,
        String status,
        long createdAt
) {
}
