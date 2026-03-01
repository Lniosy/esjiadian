package com.lniosy.usedappliance.integration;

import com.lniosy.usedappliance.integration.logistics.Kd100Gateway;
import com.lniosy.usedappliance.integration.logistics.MockLogisticsGateway;
import com.lniosy.usedappliance.integration.payment.AlipayGateway;
import com.lniosy.usedappliance.integration.payment.MockPaymentGateway;
import com.lniosy.usedappliance.integration.payment.WechatPayGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class GatewayResolverTests {

    @Test
    void resolvePaymentGatewayByChannel() {
        PaymentGatewayResolver resolver = new PaymentGatewayResolver(List.of(
                new MockPaymentGateway(), new AlipayGateway(), new WechatPayGateway()));
        Assertions.assertEquals("alipay", resolver.resolve("alipay").channel());
        Assertions.assertEquals("wechat", resolver.resolve("wechat").channel());
    }

    @Test
    void resolveLogisticsGatewayByProvider() {
        LogisticsGatewayResolver resolver = new LogisticsGatewayResolver(List.of(
                new MockLogisticsGateway(), new Kd100Gateway()), "mock");
        Assertions.assertEquals("kd100", resolver.resolve("kd100").provider());
        Assertions.assertEquals("mock", resolver.resolve("").provider());
    }
}
