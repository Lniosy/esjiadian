package com.lniosy.usedappliance.dto.refund;

import java.util.List;

public record RefundDto(
        Long id,
        Long orderId,
        Long applicantId,
        String reason,
        List<String> images,
        String status,
        String rejectReason,
        String returnCompanyCode,
        String returnTrackingNo,
        String returnShipNote,
        Long returnShippedAt,
        Long returnReceivedAt
) {
}
