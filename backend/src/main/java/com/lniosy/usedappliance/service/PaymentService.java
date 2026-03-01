package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.payment.PaymentCreateResponse;
import com.lniosy.usedappliance.entity.OrderInfo;
import com.lniosy.usedappliance.entity.PaymentRecord;
import com.lniosy.usedappliance.integration.PaymentGateway;
import com.lniosy.usedappliance.integration.PaymentGatewayResolver;
import com.lniosy.usedappliance.mapper.OrderInfoMapper;
import com.lniosy.usedappliance.mapper.PaymentRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderService orderService;
    private final PaymentGatewayResolver paymentGatewayResolver;
    private final NotificationService notificationService;

    public PaymentService(PaymentRecordMapper paymentRecordMapper, OrderInfoMapper orderInfoMapper,
                          OrderService orderService, PaymentGatewayResolver paymentGatewayResolver,
                          NotificationService notificationService) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderService = orderService;
        this.paymentGatewayResolver = paymentGatewayResolver;
        this.notificationService = notificationService;
    }

    public PaymentCreateResponse create(Long orderId, String channel) {
        PaymentGateway gateway = paymentGatewayResolver.resolve(channel);
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BizException(400, "订单状态不可支付");
        }

        String outTradeNo = channel.toUpperCase() + "-" + orderId + "-" + System.currentTimeMillis();
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(orderId);
        record.setChannel(channel);
        record.setOutTradeNo(outTradeNo);
        record.setStatus("CREATED");
        paymentRecordMapper.insert(record);

        String payUrl = gateway.createPayUrl(orderId, outTradeNo,
                order.getPaidAmount().multiply(java.math.BigDecimal.valueOf(100)).toBigInteger().toString());
        return new PaymentCreateResponse(channel, outTradeNo, payUrl, "CREATED");
    }

    @Transactional
    public void callback(String channel, Long orderId, String outTradeNo, boolean success) {
        PaymentGateway gateway = paymentGatewayResolver.resolve(channel);
        PaymentRecord record = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOutTradeNo, outTradeNo)
                .eq(PaymentRecord::getOrderId, orderId)
                .last("limit 1"));
        if (record == null) {
            throw new BizException(404, "支付记录不存在");
        }

        if (!gateway.verifyCallback("success=" + success, "mock-sign")) {
            throw new BizException(400, "支付回调验签失败");
        }

        if ("SUCCESS".equals(record.getStatus())) {
            return;
        }

        String next = success ? "SUCCESS" : "FAILED";
        paymentRecordMapper.update(null, new LambdaUpdateWrapper<PaymentRecord>()
                .eq(PaymentRecord::getId, record.getId())
                .set(PaymentRecord::getStatus, next)
                .set(PaymentRecord::getChannel, channel)
                .set(PaymentRecord::getCallbackPayload, "success=" + success));

        if (success) {
            orderService.markPaid(orderId);
            OrderInfo order = orderInfoMapper.selectById(orderId);
            if (order != null) {
                notificationService.create(order.getBuyerId(), "PAYMENT", "支付成功", "订单号: " + order.getOrderNo());
            }
        }
    }
}
