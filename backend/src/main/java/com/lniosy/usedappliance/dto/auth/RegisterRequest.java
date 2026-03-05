package com.lniosy.usedappliance.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank
        @Pattern(
                regexp = "^(1[3-9]\\d{9}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6})$",
                message = "账号必须为手机号或邮箱"
        )
        String account,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "密码至少8位且包含字母与数字") String password,
        @NotBlank String verifyCode,
        @NotNull @AssertTrue(message = "必须同意用户协议") Boolean agreed
) {
}
