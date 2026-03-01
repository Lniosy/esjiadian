package com.lniosy.usedappliance.dto.order;

import java.math.BigDecimal;

public record OrderDto(Long id, String orderNo, Long productId, Long buyerId, Long sellerId, BigDecimal paidAmount, String status) {
}
