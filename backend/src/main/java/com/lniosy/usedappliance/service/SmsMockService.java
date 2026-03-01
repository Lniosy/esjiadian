package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.dto.notification.SmsNotificationLogDto;
import com.lniosy.usedappliance.entity.SmsNotificationLog;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.SmsNotificationLogMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

@Service
public class SmsMockService {
    private final SmsNotificationLogMapper smsNotificationLogMapper;
    private final SysUserMapper sysUserMapper;

    public SmsMockService(SmsNotificationLogMapper smsNotificationLogMapper, SysUserMapper sysUserMapper) {
        this.smsNotificationLogMapper = smsNotificationLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public boolean sendImportant(Long userId, String type, String title, String content) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            return false;
        }
        SmsNotificationLog log = new SmsNotificationLog();
        log.setUserId(userId);
        log.setPhone(user.getPhone());
        log.setType(type);
        log.setTitle(title);
        log.setContent(content);
        log.setProvider("MOCK_SMS");
        log.setStatus("SENT");
        smsNotificationLogMapper.insert(log);
        return true;
    }

    public List<SmsNotificationLogDto> listByUser(Long userId, Integer limit) {
        int max = safeLimit(limit);
        return smsNotificationLogMapper.selectList(new LambdaQueryWrapper<SmsNotificationLog>()
                        .eq(SmsNotificationLog::getUserId, userId)
                        .orderByDesc(SmsNotificationLog::getId)
                        .last("limit " + max))
                .stream().map(this::toDto).toList();
    }

    public List<SmsNotificationLogDto> listAll(String type, Integer limit) {
        int max = safeLimit(limit);
        LambdaQueryWrapper<SmsNotificationLog> qw = new LambdaQueryWrapper<SmsNotificationLog>()
                .orderByDesc(SmsNotificationLog::getId)
                .last("limit " + max);
        if (type != null && !type.isBlank()) {
            qw.eq(SmsNotificationLog::getType, type);
        }
        return smsNotificationLogMapper.selectList(qw).stream().map(this::toDto).toList();
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }

    private SmsNotificationLogDto toDto(SmsNotificationLog log) {
        long ts = log.getCreatedAt() == null ? System.currentTimeMillis() :
                log.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new SmsNotificationLogDto(
                log.getId(),
                log.getUserId(),
                maskPhone(log.getPhone()),
                log.getType(),
                log.getTitle(),
                log.getContent(),
                log.getProvider(),
                log.getStatus(),
                ts
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
