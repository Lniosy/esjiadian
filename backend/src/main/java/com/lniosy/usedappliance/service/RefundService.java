package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.refund.RefundApplyRequest;
import com.lniosy.usedappliance.dto.refund.RefundDecisionRequest;
import com.lniosy.usedappliance.dto.refund.RefundDto;
import com.lniosy.usedappliance.dto.refund.RefundReturnShipRequest;
import com.lniosy.usedappliance.entity.OrderInfo;
import com.lniosy.usedappliance.entity.OrderItem;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.RefundRequest;
import com.lniosy.usedappliance.mapper.OrderInfoMapper;
import com.lniosy.usedappliance.mapper.OrderItemMapper;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.RefundRequestMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class RefundService {
    private final RefundRequestMapper refundRequestMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;

    public RefundService(RefundRequestMapper refundRequestMapper, OrderInfoMapper orderInfoMapper,
                         OrderItemMapper orderItemMapper, ProductMapper productMapper,
                         NotificationService notificationService) {
        this.refundRequestMapper = refundRequestMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public RefundDto apply(Long buyerId, Long orderId, RefundApplyRequest req) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getBuyerId().equals(buyerId)) {
            throw new BizException(404, "订单不存在");
        }
        if (!("PENDING_SHIPMENT".equals(order.getStatus()) || "PENDING_RECEIPT".equals(order.getStatus()))) {
            throw new BizException(400, "当前订单状态不可申请退款");
        }
        RefundRequest exists = refundRequestMapper.selectOne(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getOrderId, orderId)
                .last("limit 1"));
        if (exists != null && !"REJECTED".equals(exists.getStatus())) {
            throw new BizException(409, "退款申请已存在");
        }

        RefundRequest r = new RefundRequest();
        r.setOrderId(orderId);
        r.setApplicantId(buyerId);
        r.setReason(req.reason());
        r.setStatus("PENDING");
        refundRequestMapper.insert(r);

        notificationService.create(order.getSellerId(), "REFUND", "收到退款申请", "订单号: " + order.getOrderNo());
        return toDto(r);
    }

    public RefundDto detail(Long orderId, Long userId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId))) {
            throw new BizException(404, "订单不存在");
        }
        RefundRequest r = refundRequestMapper.selectOne(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getOrderId, orderId)
                .last("limit 1"));
        return r == null ? null : toDto(r);
    }

    @Transactional
    public RefundDto approve(Long operatorId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getSellerId().equals(operatorId)) {
            throw new BizException(403, "无权操作");
        }
        RefundRequest r = mustPending(orderId);
        if ("PENDING_SHIPMENT".equals(order.getStatus())) {
            refundRequestMapper.update(null, new LambdaUpdateWrapper<RefundRequest>()
                    .eq(RefundRequest::getId, r.getId())
                    .set(RefundRequest::getStatus, "COMPLETED")
                    .set(RefundRequest::getRejectReason, null));
            orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, orderId)
                    .set(OrderInfo::getStatus, "REFUNDED"));
            rollbackProductForRefund(orderId);
            notificationService.create(order.getBuyerId(), "REFUND", "退款已通过", "订单号: " + order.getOrderNo());
        } else if ("PENDING_RECEIPT".equals(order.getStatus())) {
            refundRequestMapper.update(null, new LambdaUpdateWrapper<RefundRequest>()
                    .eq(RefundRequest::getId, r.getId())
                    .set(RefundRequest::getStatus, "RETURN_REQUIRED")
                    .set(RefundRequest::getRejectReason, null));
            orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, orderId)
                    .set(OrderInfo::getStatus, "REFUNDING"));
            notificationService.create(order.getBuyerId(), "REFUND", "退款申请已同意，请寄回商品", "订单号: " + order.getOrderNo());
        } else {
            throw new BizException(400, "当前订单状态不可同意退款");
        }
        return toDto(refundRequestMapper.selectById(r.getId()));
    }

    @Transactional
    public RefundDto reject(Long operatorId, Long orderId, RefundDecisionRequest req) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getSellerId().equals(operatorId)) {
            throw new BizException(403, "无权操作");
        }
        RefundRequest r = mustPending(orderId);
        refundRequestMapper.update(null, new LambdaUpdateWrapper<RefundRequest>()
                .eq(RefundRequest::getId, r.getId())
                .set(RefundRequest::getStatus, "REJECTED")
                .set(RefundRequest::getRejectReason, req == null ? null : req.rejectReason()));

        notificationService.create(order.getBuyerId(), "REFUND", "退款被驳回", "订单号: " + order.getOrderNo());
        return toDto(refundRequestMapper.selectById(r.getId()));
    }

    @Transactional
    public RefundDto shipReturn(Long buyerId, Long orderId, RefundReturnShipRequest req) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getBuyerId().equals(buyerId)) {
            throw new BizException(404, "订单不存在");
        }
        RefundRequest r = refundRequestMapper.selectOne(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getOrderId, orderId)
                .last("limit 1"));
        if (r == null || !"RETURN_REQUIRED".equals(r.getStatus())) {
            throw new BizException(400, "当前退款状态不可填写退货物流");
        }
        refundRequestMapper.update(null, new LambdaUpdateWrapper<RefundRequest>()
                .eq(RefundRequest::getId, r.getId())
                .set(RefundRequest::getStatus, "BUYER_SHIPPED")
                .set(RefundRequest::getReturnCompanyCode, req.companyCode())
                .set(RefundRequest::getReturnTrackingNo, req.trackingNo())
                .set(RefundRequest::getReturnShipNote, req.note())
                .set(RefundRequest::getReturnShippedAt, LocalDateTime.now()));
        notificationService.create(order.getSellerId(), "REFUND", "买家已寄回退货商品", "订单号: " + order.getOrderNo());
        return toDto(refundRequestMapper.selectById(r.getId()));
    }

    @Transactional
    public RefundDto confirmReturn(Long sellerId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getSellerId().equals(sellerId)) {
            throw new BizException(404, "订单不存在");
        }
        RefundRequest r = refundRequestMapper.selectOne(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getOrderId, orderId)
                .last("limit 1"));
        if (r == null || !"BUYER_SHIPPED".equals(r.getStatus())) {
            throw new BizException(400, "当前退款状态不可确认退货");
        }
        refundRequestMapper.update(null, new LambdaUpdateWrapper<RefundRequest>()
                .eq(RefundRequest::getId, r.getId())
                .set(RefundRequest::getStatus, "COMPLETED")
                .set(RefundRequest::getReturnReceivedAt, LocalDateTime.now()));
        orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .set(OrderInfo::getStatus, "REFUNDED"));
        rollbackProductForRefund(orderId);
        notificationService.create(order.getBuyerId(), "REFUND", "卖家已确认退货，退款完成", "订单号: " + order.getOrderNo());
        return toDto(refundRequestMapper.selectById(r.getId()));
    }

    private RefundRequest mustPending(Long orderId) {
        RefundRequest r = refundRequestMapper.selectOne(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getOrderId, orderId)
                .last("limit 1"));
        if (r == null || !"PENDING".equals(r.getStatus())) {
            throw new BizException(400, "暂无待处理退款申请");
        }
        return r;
    }

    private RefundDto toDto(RefundRequest r) {
        return new RefundDto(
                r.getId(),
                r.getOrderId(),
                r.getApplicantId(),
                r.getReason(),
                r.getStatus(),
                r.getRejectReason(),
                r.getReturnCompanyCode(),
                r.getReturnTrackingNo(),
                r.getReturnShipNote(),
                toEpoch(r.getReturnShippedAt()),
                toEpoch(r.getReturnReceivedAt())
        );
    }

    private void rollbackProductForRefund(Long orderId) {
        OrderItem item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .last("limit 1"));
        if (item == null) {
            return;
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, item.getProductId())
                .set(Product::getSold, false)
                .set(Product::getStatus, "OFF_SHELF"));
    }

    private Long toEpoch(LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
