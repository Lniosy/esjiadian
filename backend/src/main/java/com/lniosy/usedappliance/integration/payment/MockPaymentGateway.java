package com.lniosy.usedappliance.integration.payment;

import com.lniosy.usedappliance.integration.PaymentGateway;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public String channel() {
        return "mock";
    }

    @Override
    public String createPayUrl(Long orderId, String outTradeNo, String amountFen) {
        return "https://pay.example.com/mock/" + outTradeNo;
    }

    @Override
    public boolean verifyCallback(String payload, String signature) {
        return true;
    }
}
