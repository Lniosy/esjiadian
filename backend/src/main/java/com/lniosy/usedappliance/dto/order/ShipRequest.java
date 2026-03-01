package com.lniosy.usedappliance.dto.order;

import jakarta.validation.constraints.NotBlank;

public record ShipRequest(@NotBlank String logisticsCompany, @NotBlank String trackingNo, String shipNote) {
}
