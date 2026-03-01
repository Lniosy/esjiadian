package com.lniosy.usedappliance.config;

import com.lniosy.usedappliance.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderLifecycleJob {
    private final OrderService orderService;

    public OrderLifecycleJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(cron = "${app.order.auto-cancel-cron:0 */5 * * * *}")
    public void autoCancelUnpaid() {
        orderService.autoCancelUnpaidOrders();
    }

    @Scheduled(cron = "${app.order.auto-receipt-cron:0 */10 * * * *}")
    public void autoConfirmReceipt() {
        orderService.autoConfirmReceiptOrders();
    }
}
