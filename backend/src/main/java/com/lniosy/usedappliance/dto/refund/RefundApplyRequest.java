package com.lniosy.usedappliance.dto.refund;

import jakarta.validation.constraints.NotBlank;

public record RefundApplyRequest(@NotBlank String reason) {
}
