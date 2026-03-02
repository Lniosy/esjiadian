package com.lniosy.usedappliance.dto.refund;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RefundApplyRequest(@NotBlank String reason, List<String> images) {
}
