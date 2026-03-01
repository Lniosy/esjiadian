package com.lniosy.usedappliance.dto.payment;

public record PaymentCreateResponse(String channel, String outTradeNo, String payUrl, String status) {
}
