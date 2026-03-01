package com.lniosy.usedappliance.dto.user;

public record UserPublicProfileDto(
        Long userId,
        String nickname,
        String avatarUrl,
        String bio,
        String authStatus
) {
}

