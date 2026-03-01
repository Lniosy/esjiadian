package com.lniosy.usedappliance.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatReportRequest(@NotBlank String reason) {
}
