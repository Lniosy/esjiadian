package com.lniosy.usedappliance.dto.notification;

public record NotificationDto(Long id, String type, String title, String content, boolean readFlag, long createdAt) {
}
