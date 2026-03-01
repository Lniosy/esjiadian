package com.lniosy.usedappliance.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String account,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "密码至少8位且包含字母与数字") String password,
        @NotBlank String verifyCode,
        @AssertTrue(message = "必须同意用户协议") boolean agreed
) {
}
