package com.lniosy.usedappliance.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank @Size(max = 150) String title,
        @NotNull Long categoryId,
        @NotBlank String brand,
        @NotBlank String model,
        LocalDate purchaseDate,
        @NotBlank String conditionLevel,
        @NotBlank String functionStatus,
        String repairHistory,
        @NotBlank @Size(max = 1000) String description,
        String videoUrl,
        @NotNull @DecimalMin("1") BigDecimal price,
        BigDecimal originalPrice,
        @NotBlank String region,
        @NotBlank String tradeMethods,
        @Size(max = 9) List<String> images
) {
}
