package com.lniosy.usedappliance.integration;

import java.util.List;

@Deprecated
public class MockLogisticsGateway implements LogisticsGateway {
    @Override
    public String provider() {
        return "legacy-mock";
    }

    @Override
    public List<String> track(String companyCode, String trackingNo) {
        return List.of("legacy: 包裹已揽收", "legacy: 包裹运输中");
    }
}
