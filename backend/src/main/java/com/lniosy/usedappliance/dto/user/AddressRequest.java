package com.lniosy.usedappliance.dto.user;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank String receiverName,
        @NotBlank String receiverPhone,
        @NotBlank String province,
        @NotBlank String city,
        @NotBlank String district,
        @NotBlank String detailAddress,
        boolean isDefault
) {
}
