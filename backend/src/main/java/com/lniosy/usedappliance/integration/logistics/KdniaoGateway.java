package com.lniosy.usedappliance.integration.logistics;

import com.lniosy.usedappliance.integration.LogisticsGateway;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KdniaoGateway implements LogisticsGateway {
    @Override
    public String provider() {
        return "kdniao";
    }

    @Override
    public List<String> track(String companyCode, String trackingNo) {
        return List.of("快递鸟: 已揽收", "快递鸟: 运输中");
    }
}
