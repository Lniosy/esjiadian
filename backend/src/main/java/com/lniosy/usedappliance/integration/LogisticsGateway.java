package com.lniosy.usedappliance.integration;

import java.util.List;

public interface LogisticsGateway {
    String provider();

    List<String> track(String companyCode, String trackingNo);
}
