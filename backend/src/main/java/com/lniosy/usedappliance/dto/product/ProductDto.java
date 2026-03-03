package com.lniosy.usedappliance.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductDto(Long id, Long sellerId, String sellerNickname, String sellerAvatarUrl,
                         String sellerShopName, String sellerShopLogo,
                         String title, Long categoryId, String brand, String model,
                         LocalDate purchaseDate, String conditionLevel, String functionStatus, String repairHistory,
                         String description, String videoUrl, BigDecimal price,
                         BigDecimal originalPrice, String region, String tradeMethods, String status,
                         String rejectReason, Boolean sold, Integer salesCount, List<String> images) {
}
