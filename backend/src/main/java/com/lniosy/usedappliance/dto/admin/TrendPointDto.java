package com.lniosy.usedappliance.dto.admin;

import java.math.BigDecimal;

public record TrendPointDto(
        String date,
        Long userCount,
        Long productCount,
        Long orderCount,
        BigDecimal gmv
) {
}
