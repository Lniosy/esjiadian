package com.lniosy.usedappliance.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(@NotBlank String nickname, String avatarUrl, String bio) {
}
