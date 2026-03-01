package com.lniosy.usedappliance.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginCodeRequest(@NotBlank String account, @NotBlank String verifyCode) {
}
