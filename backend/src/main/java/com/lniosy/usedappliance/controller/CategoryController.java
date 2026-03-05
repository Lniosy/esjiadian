package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    public ApiResponse<?> tree() {
        return ApiResponse.ok(categoryService.tree());
    }

    @GetMapping
    public ApiResponse<?> flat() {
        return ApiResponse.ok(categoryService.flat());
    }
}
