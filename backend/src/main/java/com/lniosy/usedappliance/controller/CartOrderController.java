package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.order.CartItemDto;
import com.lniosy.usedappliance.dto.order.CartItemUpdateRequest;
import com.lniosy.usedappliance.dto.order.OrderCreateRequest;
import com.lniosy.usedappliance.dto.order.OrderDto;
import com.lniosy.usedappliance.dto.order.ShipRequest;
import com.lniosy.usedappliance.service.OrderService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartOrderController {
    private final OrderService orderService;

    public CartOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/cart/items")
    public ApiResponse<List<CartItemDto>> cartList() {
        return ApiResponse.ok(orderService.listCart(SecurityUtils.currentUserId()));
    }

    @PostMapping("/cart/items")
    public ApiResponse<Void> addCart(@RequestParam Long productId) {
        orderService.addCart(SecurityUtils.currentUserId(), productId);
        return ApiResponse.ok("添加成功", null);
    }

    @DeleteMapping("/cart/items")
    public ApiResponse<Void> removeCart(@RequestParam Long productId) {
        orderService.removeCart(SecurityUtils.currentUserId(), productId);
        return ApiResponse.ok("删除成功", null);
    }

    @PutMapping("/cart/items")
    public ApiResponse<Void> updateCart(@RequestBody @Valid CartItemUpdateRequest req) {
        orderService.updateCartItem(SecurityUtils.currentUserId(), req);
        return ApiResponse.ok("更新成功", null);
    }

    @PostMapping("/orders")
    public ApiResponse<OrderDto> createOrder(@RequestBody @Valid OrderCreateRequest req) {
        return ApiResponse.ok(orderService.createOrder(SecurityUtils.currentUserId(), req));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderDto>> listOrders(@RequestParam(defaultValue = "buyer") String view) {
        return ApiResponse.ok(orderService.listOrders(SecurityUtils.currentUserId(), view));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderDto> getOrder(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getOrder(id, SecurityUtils.currentUserId()));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        orderService.cancel(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok("取消成功", null);
    }

    @PostMapping("/orders/{id}/ship")
    public ApiResponse<Void> ship(@PathVariable Long id, @RequestBody @Valid ShipRequest req) {
        orderService.ship(SecurityUtils.currentUserId(), id, req);
        return ApiResponse.ok("发货成功", null);
    }

    @PostMapping("/orders/{id}/confirm-receipt")
    public ApiResponse<Void> confirmReceipt(@PathVariable Long id) {
        orderService.confirmReceipt(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok("确认收货成功", null);
    }
}
