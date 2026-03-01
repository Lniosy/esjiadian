package com.lniosy.usedappliance.dto.shop;

import com.lniosy.usedappliance.dto.evaluation.EvaluationDto;

import java.util.List;

public record ShopOverviewDto(
        ShopDto shop,
        Double rating,
        Long evaluationCount,
        Long onShelfProductCount,
        List<ShopOverviewProductDto> products,
        List<EvaluationDto> evaluations
) {
}
