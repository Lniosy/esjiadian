package com.lniosy.usedappliance.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(@NotNull Long productId, @NotNull Long addressId, @NotBlank String tradeMethod, String buyerNote) {
}
