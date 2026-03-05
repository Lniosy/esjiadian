package com.lniosy.usedappliance.dto.evaluation;

import jakarta.validation.constraints.*;

import java.util.List;

public record EvaluationRequest(
        @NotNull Long productId,
        @Min(1) @Max(5) int score,
        @NotBlank @Size(min = 10, max = 500) String content,
        @Size(max = 6) List<String> images,
        List<String> tags
) {
}
