package com.lniosy.usedappliance.integration.logistics;

import com.lniosy.usedappliance.integration.LogisticsGateway;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Kd100Gateway implements LogisticsGateway {
    @Override
    public String provider() {
        return "kd100";
    }

    @Override
    public List<String> track(String companyCode, String trackingNo) {
        return List.of("快递100: 已揽收", "快递100: 运输中");
    }
}
