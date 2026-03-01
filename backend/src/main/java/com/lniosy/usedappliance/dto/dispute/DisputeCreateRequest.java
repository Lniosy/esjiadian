package com.lniosy.usedappliance.dto.dispute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DisputeCreateRequest(@NotNull Long orderId, @NotBlank String reason) {
}
