package com.lniosy.usedappliance.config;

import com.lniosy.usedappliance.service.MonitorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitorAlertJob {
    private final MonitorService monitorService;

    public MonitorAlertJob(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Scheduled(cron = "${app.monitor.alert.cron:0 */5 * * * *}")
    public void checkAndNotify() {
        monitorService.checkAndNotifyAlerts();
    }
}
