package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.dispute.DisputeCreateRequest;
import com.lniosy.usedappliance.dto.dispute.DisputeDto;
import com.lniosy.usedappliance.service.DisputeService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {
    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ApiResponse<DisputeDto> create(@RequestBody @Valid DisputeCreateRequest req) {
        return ApiResponse.ok(disputeService.create(SecurityUtils.currentUserId(), req));
    }

    @GetMapping
    public ApiResponse<List<DisputeDto>> mine() {
        return ApiResponse.ok(disputeService.myDisputes(SecurityUtils.currentUserId()));
    }
}
