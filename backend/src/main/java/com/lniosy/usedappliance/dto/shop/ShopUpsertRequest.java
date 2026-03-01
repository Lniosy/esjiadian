package com.lniosy.usedappliance.dto.shop;

import jakarta.validation.constraints.NotBlank;

public record ShopUpsertRequest(
        @NotBlank String name,
        String logoUrl,
        String intro,
        String categories,
        String region
) {
}
