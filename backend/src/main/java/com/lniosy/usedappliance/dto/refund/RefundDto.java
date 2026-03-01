package com.lniosy.usedappliance.dto.refund;

public record RefundDto(
        Long id,
        Long orderId,
        Long applicantId,
        String reason,
        String status,
        String rejectReason,
        String returnCompanyCode,
        String returnTrackingNo,
        String returnShipNote,
        Long returnShippedAt,
        Long returnReceivedAt
) {
}
