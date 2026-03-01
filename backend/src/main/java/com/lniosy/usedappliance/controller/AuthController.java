package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.auth.*;
import com.lniosy.usedappliance.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/code/send")
    public ApiResponse<SendCodeResponse> sendCode(@RequestBody @Valid SendCodeRequest req) {
        return ApiResponse.ok(authService.sendVerifyCode(req.account()));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody @Valid RegisterRequest req) {
        authService.register(req);
        return ApiResponse.ok("注册成功", null);
    }

    @PostMapping("/login/password")
    public ApiResponse<AuthTokenResponse> loginByPassword(@RequestBody @Valid LoginPasswordRequest req) {
        return ApiResponse.ok(authService.loginByPassword(req));
    }

    @PostMapping("/login/code")
    public ApiResponse<AuthTokenResponse> loginByCode(@RequestBody @Valid LoginCodeRequest req) {
        return ApiResponse.ok(authService.loginByCode(req));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok("退出成功", null);
    }
}
