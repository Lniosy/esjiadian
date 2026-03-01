package com.lniosy.usedappliance.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank String title,
        @NotNull Long categoryId,
        @NotBlank String brand,
        @NotBlank String model,
        LocalDate purchaseDate,
        @NotBlank String conditionLevel,
        @NotBlank String functionStatus,
        String repairHistory,
        @NotBlank String description,
        String videoUrl,
        @NotNull @DecimalMin("1") BigDecimal price,
        BigDecimal originalPrice,
        @NotBlank String region,
        @NotBlank String tradeMethods,
        List<String> images
) {
}
