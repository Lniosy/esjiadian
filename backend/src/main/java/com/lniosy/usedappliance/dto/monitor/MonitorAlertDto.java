package com.lniosy.usedappliance.dto.monitor;

public record MonitorAlertDto(
        String key,
        String level,
        String title,
        String message,
        double currentValue,
        double threshold,
        long detectedAt,
        long expireAt
) {
}
