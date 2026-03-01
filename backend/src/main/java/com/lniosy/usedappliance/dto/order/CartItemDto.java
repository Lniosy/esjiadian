package com.lniosy.usedappliance.dto.order;

import java.math.BigDecimal;

public record CartItemDto(
        Long productId,
        String title,
        BigDecimal price,
        Integer quantity,
        Boolean selected,
        String productStatus,
        boolean valid,
        String invalidReason
) {
}
