package com.lniosy.usedappliance.dto.user;

public record UserProfileDto(
        Long userId,
        String nickname,
        String avatarUrl,
        String bio,
        String roles,
        String authStatus,
        Boolean enabled
) {
}
