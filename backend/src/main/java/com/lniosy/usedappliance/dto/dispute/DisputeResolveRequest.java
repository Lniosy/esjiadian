package com.lniosy.usedappliance.dto.dispute;

import jakarta.validation.constraints.NotBlank;

public record DisputeResolveRequest(@NotBlank String status, String resultNote) {
}
