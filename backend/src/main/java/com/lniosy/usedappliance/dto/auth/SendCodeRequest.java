package com.lniosy.usedappliance.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record SendCodeRequest(@NotBlank String account) {
}
