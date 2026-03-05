package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.common.PageResult;
import com.lniosy.usedappliance.dto.product.ProductCreateRequest;
import com.lniosy.usedappliance.dto.product.ProductDto;
import com.lniosy.usedappliance.dto.product.ProductQuery;
import com.lniosy.usedappliance.service.ProductService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ApiResponse<ProductDto> create(@RequestBody @Valid ProductCreateRequest req) {
        return ApiResponse.ok(productService.create(SecurityUtils.currentUserId(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDto> update(@PathVariable Long id, @RequestBody @Valid ProductCreateRequest req) {
        return ApiResponse.ok(productService.update(SecurityUtils.currentUserId(), id, req));
    }

    @PostMapping("/{id}/on-shelf")
    public ApiResponse<Void> onShelf(@PathVariable Long id) {
        productService.onShelf(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok("上架成功", null);
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<Void> offShelf(@PathVariable Long id) {
        productService.offShelf(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok("下架成功", null);
    }

    @GetMapping
    public ApiResponse<PageResult<ProductDto>> list(@ModelAttribute ProductQuery query) {
        return ApiResponse.ok(productService.list(query, SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id, SecurityUtils.currentUserId()));
    }
}
