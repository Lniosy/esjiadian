package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.dto.logistics.LogisticsDto;
import com.lniosy.usedappliance.dto.logistics.LogisticsTrackPointDto;
import com.lniosy.usedappliance.entity.LogisticsRecord;
import com.lniosy.usedappliance.entity.LogisticsTrack;
import com.lniosy.usedappliance.entity.OrderInfo;
import com.lniosy.usedappliance.integration.LogisticsGateway;
import com.lniosy.usedappliance.integration.LogisticsGatewayResolver;
import com.lniosy.usedappliance.mapper.LogisticsRecordMapper;
import com.lniosy.usedappliance.mapper.LogisticsTrackMapper;
import com.lniosy.usedappliance.mapper.OrderInfoMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LogisticsService {
    private static final DateTimeFormatter TRACK_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final LogisticsRecordMapper logisticsRecordMapper;
    private final LogisticsTrackMapper logisticsTrackMapper;
    private final LogisticsGatewayResolver logisticsGatewayResolver;
    private final OrderInfoMapper orderInfoMapper;
    private final NotificationService notificationService;
    private final String provider;

    public LogisticsService(LogisticsRecordMapper logisticsRecordMapper,
                            LogisticsTrackMapper logisticsTrackMapper,
                            LogisticsGatewayResolver logisticsGatewayResolver,
                            OrderInfoMapper orderInfoMapper,
                            NotificationService notificationService,
                            @Value("${app.logistics.provider:mock}") String provider) {
        this.logisticsRecordMapper = logisticsRecordMapper;
        this.logisticsTrackMapper = logisticsTrackMapper;
        this.logisticsGatewayResolver = logisticsGatewayResolver;
        this.orderInfoMapper = orderInfoMapper;
        this.notificationService = notificationService;
        this.provider = provider;
    }

    @Transactional
    public LogisticsDto save(Long orderId, String companyCode, String trackingNo, String note) {
        LogisticsRecord record = logisticsRecordMapper.selectOne(new LambdaQueryWrapper<LogisticsRecord>()
                .eq(LogisticsRecord::getOrderId, orderId)
                .last("limit 1"));

        if (record == null) {
            record = new LogisticsRecord();
            record.setOrderId(orderId);
            record.setCompanyCode(companyCode);
            record.setTrackingNo(trackingNo);
            record.setStatus("IN_TRANSIT");
            record.setShipNote(note);
            logisticsRecordMapper.insert(record);
        } else {
            logisticsRecordMapper.update(null, new LambdaUpdateWrapper<LogisticsRecord>()
                    .eq(LogisticsRecord::getId, record.getId())
                    .set(LogisticsRecord::getCompanyCode, companyCode)
                    .set(LogisticsRecord::getTrackingNo, trackingNo)
                    .set(LogisticsRecord::getStatus, "IN_TRANSIT")
                    .set(LogisticsRecord::getShipNote, note));
            record = logisticsRecordMapper.selectById(record.getId());
        }

        appendTracks(record);
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order != null) {
            notificationService.create(order.getBuyerId(), "LOGISTICS", "物流状态更新", "订单号: " + order.getOrderNo());
        }
        return tracks(orderId);
    }

    @Transactional
    public void syncInTransitTracks() {
        List<LogisticsRecord> inTransit = logisticsRecordMapper.selectList(new LambdaQueryWrapper<LogisticsRecord>()
                .eq(LogisticsRecord::getStatus, "IN_TRANSIT"));
        for (LogisticsRecord record : inTransit) {
            appendTracks(record);
        }
    }

    public LogisticsDto tracks(Long orderId) {
        LogisticsRecord record = logisticsRecordMapper.selectOne(new LambdaQueryWrapper<LogisticsRecord>()
                .eq(LogisticsRecord::getOrderId, orderId)
                .last("limit 1"));
        if (record == null) {
            return new LogisticsDto(orderId, "", "", "CREATED", "暂无轨迹", List.of());
        }

        List<LogisticsTrack> allTracks = logisticsTrackMapper.selectList(new LambdaQueryWrapper<LogisticsTrack>()
                .eq(LogisticsTrack::getLogisticsId, record.getId())
                .orderByDesc(LogisticsTrack::getTrackTime));

        List<LogisticsTrackPointDto> trackItems = allTracks.stream()
                .map(t -> new LogisticsTrackPointDto(
                        t.getContent(),
                        t.getTrackTime() == null ? "" : t.getTrackTime().format(TRACK_TIME_FORMATTER),
                        t.getStatus()
                ))
                .toList();

        String latestTrack = trackItems.isEmpty() ? "暂无轨迹" : trackItems.get(0).description();
        return new LogisticsDto(orderId, record.getCompanyCode(), record.getTrackingNo(), record.getStatus(), latestTrack, trackItems);
    }

    private void appendTracks(LogisticsRecord record) {
        LogisticsGateway gateway = logisticsGatewayResolver.resolve(provider);
        List<String> tracks = gateway.track(record.getCompanyCode(), record.getTrackingNo());
        for (String t : tracks) {
            boolean exists = logisticsTrackMapper.selectCount(new LambdaQueryWrapper<LogisticsTrack>()
                    .eq(LogisticsTrack::getLogisticsId, record.getId())
                    .eq(LogisticsTrack::getContent, t)) > 0;
            if (!exists) {
                LogisticsTrack track = new LogisticsTrack();
                track.setLogisticsId(record.getId());
                track.setTrackTime(LocalDateTime.now());
                track.setContent(t);
                track.setStatus("IN_TRANSIT");
                logisticsTrackMapper.insert(track);
            }
        }
    }
}
