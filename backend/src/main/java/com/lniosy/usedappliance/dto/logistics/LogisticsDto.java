package com.lniosy.usedappliance.dto.logistics;

import java.util.List;

public record LogisticsDto(
        Long orderId,
        String companyCode,
        String trackingNo,
        String status,
        String latestTrack,
        List<LogisticsTrackPointDto> tracks
) {
}
