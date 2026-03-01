package com.lniosy.usedappliance.dto.user;

import jakarta.validation.constraints.NotBlank;

public record RealNameVerifyRequest(@NotBlank String realName, @NotBlank String idCard, @NotBlank String phone) {
}
