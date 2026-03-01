package com.lniosy.usedappliance.dto.logistics;

public record LogisticsDto(Long orderId, String companyCode, String trackingNo, String status, String latestTrack) {
}
