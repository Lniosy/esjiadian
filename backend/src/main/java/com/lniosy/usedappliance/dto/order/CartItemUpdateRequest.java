package com.lniosy.usedappliance.dto.order;

import jakarta.validation.constraints.NotNull;

public record CartItemUpdateRequest(
        @NotNull(message = "商品ID不能为空") Long productId,
        Integer quantity,
        Boolean selected
) {
}
