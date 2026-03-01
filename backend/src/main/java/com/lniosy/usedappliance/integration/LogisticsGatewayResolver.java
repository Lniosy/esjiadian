package com.lniosy.usedappliance.integration;

import com.lniosy.usedappliance.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogisticsGatewayResolver {
    private final List<LogisticsGateway> gateways;
    private final String defaultProvider;

    public LogisticsGatewayResolver(List<LogisticsGateway> gateways,
                                    @Value("${app.logistics.provider:mock}") String defaultProvider) {
        this.gateways = gateways;
        this.defaultProvider = defaultProvider;
    }

    public LogisticsGateway resolve(String provider) {
        String target = (provider == null || provider.isBlank()) ? defaultProvider : provider;
        return gateways.stream()
                .filter(g -> g.provider().equalsIgnoreCase(target))
                .findFirst()
                .orElseThrow(() -> new BizException(400, "不支持的物流提供商: " + target));
    }
}
