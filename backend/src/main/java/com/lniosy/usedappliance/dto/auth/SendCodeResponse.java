package com.lniosy.usedappliance.dto.auth;

public record SendCodeResponse(String account, String verifyCode, long expiresInSeconds, boolean mock) {
}
