package com.lniosy.usedappliance.dto.evaluation;

import java.util.List;

public record EvaluationDto(
        Long orderId,
        Long productId,
        Long sellerId,
        Long buyerId,
        int score,
        String content,
        List<String> images,
        List<String> tags,
        long createdAt
) {
}
