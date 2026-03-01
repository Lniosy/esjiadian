package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.monitor.MonitorAlertDto;
import com.lniosy.usedappliance.dto.monitor.MonitorAlertEventDto;
import com.lniosy.usedappliance.entity.MonitorAlertEvent;
import com.lniosy.usedappliance.mapper.MonitorAlertEventMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MonitorAlertEventService {
    private final MonitorAlertEventMapper mapper;

    public MonitorAlertEventService(MonitorAlertEventMapper mapper) {
        this.mapper = mapper;
    }

    public void syncCurrentAlerts(List<MonitorAlertDto> current) {
        Set<String> activeKeys = new HashSet<>();
        for (MonitorAlertDto alert : current) {
            activeKeys.add(alert.key());
            MonitorAlertEvent open = mapper.selectOne(new LambdaQueryWrapper<MonitorAlertEvent>()
                    .eq(MonitorAlertEvent::getAlertKey, alert.key())
                    .eq(MonitorAlertEvent::getStatus, "OPEN")
                    .last("limit 1"));
            if (open == null) {
                MonitorAlertEvent e = new MonitorAlertEvent();
                e.setAlertKey(alert.key());
                e.setLevel(alert.level());
                e.setTitle(alert.title());
                e.setMessage(alert.message());
                e.setCurrentValue(alert.currentValue());
                e.setThresholdValue(alert.threshold());
                e.setStatus("OPEN");
                mapper.insert(e);
            } else {
                mapper.update(null, new LambdaUpdateWrapper<MonitorAlertEvent>()
                        .eq(MonitorAlertEvent::getId, open.getId())
                        .set(MonitorAlertEvent::getLevel, alert.level())
                        .set(MonitorAlertEvent::getTitle, alert.title())
                        .set(MonitorAlertEvent::getMessage, alert.message())
                        .set(MonitorAlertEvent::getCurrentValue, alert.currentValue())
                        .set(MonitorAlertEvent::getThresholdValue, alert.threshold()));
            }
        }
        // close disappeared alerts
        if (!activeKeys.isEmpty()) {
            mapper.update(null, new LambdaUpdateWrapper<MonitorAlertEvent>()
                    .eq(MonitorAlertEvent::getStatus, "OPEN")
                    .notIn(MonitorAlertEvent::getAlertKey, activeKeys)
                    .set(MonitorAlertEvent::getStatus, "RESOLVED"));
        } else {
            mapper.update(null, new LambdaUpdateWrapper<MonitorAlertEvent>()
                    .eq(MonitorAlertEvent::getStatus, "OPEN")
                    .set(MonitorAlertEvent::getStatus, "RESOLVED"));
        }
    }

    public List<MonitorAlertEventDto> list(String status) {
        String s = (status == null || status.isBlank()) ? "OPEN" : status.toUpperCase();
        return mapper.selectList(new LambdaQueryWrapper<MonitorAlertEvent>()
                        .eq(MonitorAlertEvent::getStatus, s)
                        .orderByDesc(MonitorAlertEvent::getUpdatedAt))
                .stream().map(this::toDto).toList();
    }

    public void ack(Long id, Long adminId) {
        MonitorAlertEvent event = mapper.selectById(id);
        if (event == null) {
            throw new BizException(404, "告警不存在");
        }
        if (!"OPEN".equals(event.getStatus())) {
            return;
        }
        mapper.update(null, new LambdaUpdateWrapper<MonitorAlertEvent>()
                .eq(MonitorAlertEvent::getId, id)
                .set(MonitorAlertEvent::getStatus, "ACKED")
                .set(MonitorAlertEvent::getAckBy, adminId)
                .set(MonitorAlertEvent::getAckAt, LocalDateTime.now()));
    }

    private MonitorAlertEventDto toDto(MonitorAlertEvent e) {
        return new MonitorAlertEventDto(
                e.getId(),
                e.getAlertKey(),
                e.getLevel(),
                e.getTitle(),
                e.getMessage(),
                e.getStatus(),
                e.getAckBy(),
                toEpoch(e.getAckAt()),
                toEpoch(e.getUpdatedAt())
        );
    }

    private Long toEpoch(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
