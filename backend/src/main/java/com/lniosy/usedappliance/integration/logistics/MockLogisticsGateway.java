package com.lniosy.usedappliance.integration.logistics;

import com.lniosy.usedappliance.integration.LogisticsGateway;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockLogisticsGateway implements LogisticsGateway {
    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public List<String> track(String companyCode, String trackingNo) {
        return List.of("包裹已揽收", "包裹运输中");
    }
}
