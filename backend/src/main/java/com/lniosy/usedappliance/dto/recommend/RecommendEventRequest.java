package com.lniosy.usedappliance.dto.recommend;

import jakarta.validation.constraints.NotBlank;

public record RecommendEventRequest(
        Long productId,
        Long categoryId,

        @NotBlank(message = "事件类型不能为空")
        String eventType,

        Double eventScore
) {
}
