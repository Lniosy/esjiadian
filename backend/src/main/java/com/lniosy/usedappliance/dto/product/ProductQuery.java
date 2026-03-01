package com.lniosy.usedappliance.dto.product;

import java.math.BigDecimal;

public record ProductQuery(
        String keyword,
        Long sellerId,
        Long categoryId,
        String conditionLevel,
        String functionStatus,
        String region,
        String tradeMethod,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sortBy,
        Integer pageNum,
        Integer pageSize
) {
}
