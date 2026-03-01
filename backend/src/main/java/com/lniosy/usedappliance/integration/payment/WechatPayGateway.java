package com.lniosy.usedappliance.integration.payment;

import com.lniosy.usedappliance.integration.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WechatPayGateway implements PaymentGateway {

    @Value("${app.payment.wechat.mock-base-url:https://pay.example.com/wechat}")
    private String baseUrl;

    @Override
    public String channel() {
        return "wechat";
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
