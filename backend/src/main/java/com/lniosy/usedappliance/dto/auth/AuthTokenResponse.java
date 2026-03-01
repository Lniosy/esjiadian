package com.lniosy.usedappliance.dto.auth;

public record AuthTokenResponse(String token, Long userId, String username, String roles) {
}
