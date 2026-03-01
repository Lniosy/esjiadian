package com.lniosy.usedappliance.integration;

import com.lniosy.usedappliance.common.BizException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentGatewayResolver {
    private final List<PaymentGateway> gateways;

    public PaymentGatewayResolver(List<PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    public PaymentGateway resolve(String channel) {
        return gateways.stream()
                .filter(g -> g.channel().equalsIgnoreCase(channel))
                .findFirst()
                .orElseThrow(() -> new BizException(400, "不支持的支付渠道: " + channel));
    }
}
