package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.user.*;
import com.lniosy.usedappliance.service.UserService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/me")
    public ApiResponse<UserProfileDto> me() {
        return ApiResponse.ok(userService.getProfile(SecurityUtils.currentUserId()));
    }

    @GetMapping("/users/{id}/public")
    public ApiResponse<UserPublicProfileDto> publicProfile(@PathVariable Long id) {
        return ApiResponse.ok(userService.getPublicProfile(id));
    }

    @PutMapping("/users/me")
    public ApiResponse<UserProfileDto> update(@RequestBody @Valid UpdateProfileRequest req) {
        return ApiResponse.ok(userService.updateProfile(SecurityUtils.currentUserId(), req));
    }

    @GetMapping("/users/me/addresses")
    public ApiResponse<List<AddressDto>> addresses() {
        return ApiResponse.ok(userService.listAddresses(SecurityUtils.currentUserId()));
    }

    @PostMapping("/users/me/addresses")
    public ApiResponse<AddressDto> addAddress(@RequestBody @Valid AddressRequest req) {
        return ApiResponse.ok(userService.addAddress(SecurityUtils.currentUserId(), req));
    }

    @PutMapping("/users/me/addresses/{id}")
    public ApiResponse<AddressDto> updateAddress(@PathVariable Long id, @RequestBody @Valid AddressRequest req) {
        return ApiResponse.ok(userService.updateAddress(SecurityUtils.currentUserId(), id, req));
    }

    @DeleteMapping("/users/me/addresses/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id) {
        userService.deleteAddress(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/verification/real-name")
    public ApiResponse<Map<String, String>> applyRealName(@RequestBody @Valid RealNameVerifyRequest req) {
        return ApiResponse.ok(Map.of("status", userService.applyRealName(SecurityUtils.currentUserId(), req)));
    }

    @GetMapping("/verification/status")
    public ApiResponse<Map<String, String>> realNameStatus() {
        return ApiResponse.ok(Map.of("status", userService.realNameStatus(SecurityUtils.currentUserId())));
    }
}
