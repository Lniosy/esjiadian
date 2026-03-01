package com.lniosy.usedappliance.dto.monitor;

public record MonitorAlertEventDto(
        Long id,
        String alertKey,
        String level,
        String title,
        String message,
        String status,
        Long ackBy,
        Long ackAt,
        Long updatedAt
) {
}
