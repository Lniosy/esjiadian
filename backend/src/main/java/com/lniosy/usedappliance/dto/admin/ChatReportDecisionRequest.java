package com.lniosy.usedappliance.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record ChatReportDecisionRequest(
        @NotBlank(message = "状态不能为空")
        String status,
        String note
) {
}
