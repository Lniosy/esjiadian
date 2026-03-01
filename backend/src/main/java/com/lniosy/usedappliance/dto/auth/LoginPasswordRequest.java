package com.lniosy.usedappliance.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginPasswordRequest(@NotBlank String account, @NotBlank String password) {
}
