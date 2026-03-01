package com.lniosy.usedappliance.integration;

@Deprecated
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public String channel() {
        return "legacy-mock";
    }

    @Override
    public String createPayUrl(Long orderId, String outTradeNo, String amountFen) {
        return "https://pay.example.com/legacy/" + outTradeNo;
    }

    @Override
    public boolean verifyCallback(String payload, String signature) {
        return true;
    }
}
