package com.lniosy.usedappliance.dto.evaluation;

import java.util.List;
import java.util.Map;

public record RatingDetailDto(
        double rating,
        long total,
        Map<Integer, Long> distribution,
        List<Map<String, Object>> tagCloud
) {
}
