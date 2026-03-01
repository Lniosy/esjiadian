package com.lniosy.usedappliance.service;

import com.lniosy.usedappliance.dto.chat.ChatMessageDto;
import com.lniosy.usedappliance.dto.evaluation.EvaluationDto;
import com.lniosy.usedappliance.dto.order.OrderDto;
import com.lniosy.usedappliance.dto.product.ProductDto;
import com.lniosy.usedappliance.dto.user.AddressDto;
import com.lniosy.usedappliance.dto.user.UserProfileDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryStore {
    public final AtomicLong userId = new AtomicLong(1);
    public final AtomicLong productId = new AtomicLong(1);
    public final AtomicLong orderId = new AtomicLong(1);
    public final AtomicLong addressId = new AtomicLong(1);

    public final Map<String, UserCredential> credentialByAccount = new ConcurrentHashMap<>();
    public final Map<Long, UserProfileDto> profileByUserId = new ConcurrentHashMap<>();
    public final Map<Long, List<AddressDto>> addressByUserId = new ConcurrentHashMap<>();
    public final Map<Long, ProductDto> productById = new ConcurrentHashMap<>();
    public final Map<Long, OrderDto> orderById = new ConcurrentHashMap<>();
    public final Map<Long, List<Long>> cartByUserId = new ConcurrentHashMap<>();
    public final List<ChatMessageDto> chatMessages = new ArrayList<>();
    public final List<EvaluationDto> evaluations = new ArrayList<>();

    public record UserCredential(Long userId, String account, String passwordHash, String roles, String authStatus, int failCount, long lockUntil) {
    }
}
