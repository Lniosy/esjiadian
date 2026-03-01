package com.lniosy.usedappliance.dto.refund;

import jakarta.validation.constraints.NotBlank;

public record RefundReturnShipRequest(
        @NotBlank String companyCode,
        @NotBlank String trackingNo,
        String note
) {
}
