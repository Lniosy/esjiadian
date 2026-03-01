package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.dto.dispute.DisputeCreateRequest;
import com.lniosy.usedappliance.dto.dispute.DisputeDto;
import com.lniosy.usedappliance.dto.dispute.DisputeResolveRequest;
import com.lniosy.usedappliance.entity.DisputeCase;
import com.lniosy.usedappliance.mapper.DisputeCaseMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DisputeService {
    private final DisputeCaseMapper disputeCaseMapper;
    private final NotificationService notificationService;

    public DisputeService(DisputeCaseMapper disputeCaseMapper, NotificationService notificationService) {
        this.disputeCaseMapper = disputeCaseMapper;
        this.notificationService = notificationService;
    }

    public DisputeDto create(Long applicantId, DisputeCreateRequest req) {
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
