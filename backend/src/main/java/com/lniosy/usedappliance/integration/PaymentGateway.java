package com.lniosy.usedappliance.integration;

public interface PaymentGateway {
    String channel();

    String createPayUrl(Long orderId, String outTradeNo, String amountFen);

    boolean verifyCallback(String payload, String signature);
}
