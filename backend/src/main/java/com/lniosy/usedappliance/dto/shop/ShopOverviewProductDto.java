package com.lniosy.usedappliance.dto.shop;

import java.math.BigDecimal;

public record ShopOverviewProductDto(
        Long id,
        String title,
        BigDecimal price,
        String status,
        String image
) {
}
