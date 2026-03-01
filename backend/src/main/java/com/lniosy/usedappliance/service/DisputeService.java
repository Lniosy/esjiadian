package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.dispute.DisputeCreateRequest;
import com.lniosy.usedappliance.dto.dispute.DisputeDto;
import com.lniosy.usedappliance.dto.dispute.DisputeResolveRequest;
import com.lniosy.usedappliance.entity.DisputeCase;
import com.lniosy.usedappliance.entity.OrderInfo;
import com.lniosy.usedappliance.mapper.DisputeCaseMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DisputeService {
    private final DisputeCaseMapper disputeCaseMapper;
    private final NotificationService notificationService;
    private final OrderService orderService;

    public DisputeService(DisputeCaseMapper disputeCaseMapper, NotificationService notificationService, OrderService orderService) {
        this.disputeCaseMapper = disputeCaseMapper;
        this.notificationService = notificationService;
        this.orderService = orderService;
    }

    public DisputeDto create(Long applicantId, DisputeCreateRequest req) {
        OrderInfo order = orderService.getOrderEntity(req.orderId());
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        boolean participant = applicantId.equals(order.getBuyerId()) || applicantId.equals(order.getSellerId());
        if (!participant) {
            throw new BizException(403, "只能对本人参与的订单发起纠纷");
        }
        if ("PENDING_PAYMENT".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BizException(400, "当前订单状态不支持发起纠纷");
        }
        Long exists = disputeCaseMapper.selectCount(new LambdaQueryWrapper<DisputeCase>()
                .eq(DisputeCase::getOrderId, req.orderId())
                .eq(DisputeCase::getApplicantId, applicantId)
                .in(DisputeCase::getStatus, List.of("OPEN", "PENDING", "PROCESSING")));
        if (exists != null && exists > 0) {
            throw new BizException(400, "该订单已有处理中纠纷");
        }
        DisputeCase d = new DisputeCase();
        d.setOrderId(req.orderId());
        d.setApplicantId(applicantId);
        d.setReason(req.reason());
        d.setStatus("OPEN");
        disputeCaseMapper.insert(d);
        notificationService.create(applicantId, "DISPUTE", "纠纷已提交", "纠纷单号: " + d.getId());
        return toDto(d);
    }

    public List<DisputeDto> myDisputes(Long applicantId) {
        return disputeCaseMapper.selectList(new LambdaQueryWrapper<DisputeCase>()
                        .eq(DisputeCase::getApplicantId, applicantId)
                        .orderByDesc(DisputeCase::getId))
                .stream().map(this::toDto).toList();
    }

    public List<DisputeDto> allDisputes() {
        return disputeCaseMapper.selectList(new LambdaQueryWrapper<DisputeCase>().orderByDesc(DisputeCase::getId))
                .stream().map(this::toDto).toList();
    }

    public DisputeDto resolve(Long disputeId, DisputeResolveRequest req) {
        disputeCaseMapper.update(null, new LambdaUpdateWrapper<DisputeCase>()
                .eq(DisputeCase::getId, disputeId)
                .set(DisputeCase::getStatus, req.status())
                .set(DisputeCase::getResultNote, req.resultNote()));
        DisputeCase d = disputeCaseMapper.selectById(disputeId);
        if (d != null) {
            notificationService.create(d.getApplicantId(), "DISPUTE", "纠纷处理结果", req.status() + " - " + (req.resultNote() == null ? "" : req.resultNote()));
        }
        return d == null ? null : toDto(d);
    }

    private DisputeDto toDto(DisputeCase d) {
        return new DisputeDto(d.getId(), d.getOrderId(), d.getApplicantId(), d.getReason(), d.getStatus(), d.getResultNote());
    }
}
