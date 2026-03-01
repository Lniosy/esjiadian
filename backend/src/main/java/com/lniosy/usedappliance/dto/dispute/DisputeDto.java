package com.lniosy.usedappliance.dto.dispute;

public record DisputeDto(Long id, Long orderId, Long applicantId, String reason, String status, String resultNote) {
}
