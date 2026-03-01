package com.lniosy.usedappliance.config;

import com.lniosy.usedappliance.service.LogisticsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LogisticsSyncJob {
    private final LogisticsService logisticsService;

    public LogisticsSyncJob(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @Scheduled(cron = "${app.logistics.sync-cron:0 */5 * * * *}")
    public void syncTracks() {
        logisticsService.syncInTransitTracks();
    }
}
