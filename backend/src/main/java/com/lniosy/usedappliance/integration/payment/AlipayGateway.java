package com.lniosy.usedappliance.integration.payment;

import com.lniosy.usedappliance.integration.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlipayGateway implements PaymentGateway {

    @Value("${app.payment.alipay.mock-base-url:https://pay.example.com/alipay}")
    private String baseUrl;

    @Override
    public String channel() {
        return "alipay";
    }

    @Override
    public String createPayUrl(Long orderId, String outTradeNo, String amountFen) {
        return baseUrl + "/" + outTradeNo + "?amountFen=" + amountFen;
    }

    @Override
    public boolean verifyCallback(String payload, String signature) {
        return true;
    }
}
