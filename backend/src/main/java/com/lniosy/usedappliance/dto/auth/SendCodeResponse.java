package com.lniosy.usedappliance.dto.auth;

public record SendCodeResponse(String account, String code, String verifyCode, long expiresInSeconds, boolean mock) {
    public static SendCodeResponse of(String account, String verifyCode, long expiresInSeconds, boolean mock) {
        return new SendCodeResponse(account, verifyCode, verifyCode, expiresInSeconds, mock);
    }
}
