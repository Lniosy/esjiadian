package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.dto.notification.NotificationDto;
import com.lniosy.usedappliance.entity.Notification;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.NotificationMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final SysUserMapper sysUserMapper;
    private final SmsMockService smsMockService;
    private static final Set<String> IMPORTANT_SMS_TYPES = Set.of(
            "ORDER", "PAYMENT", "REFUND", "DISPUTE", "LOGISTICS", "MONITOR_ALERT"
    );

    public NotificationService(NotificationMapper notificationMapper, SysUserMapper sysUserMapper,
                               SmsMockService smsMockService) {
        this.notificationMapper = notificationMapper;
        this.sysUserMapper = sysUserMapper;
        this.smsMockService = smsMockService;
    }

    public void create(Long userId, String type, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setReadFlag(false);
        notificationMapper.insert(n);
        if (IMPORTANT_SMS_TYPES.contains(type)) {
            smsMockService.sendImportant(userId, type, title, content);
        }
    }

    public List<NotificationDto> list(Long userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getId))
                .stream().map(this::toDto).toList();
    }

    public void markRead(Long userId, Long id) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getReadFlag, true));
    }

    public void createForAdmins(String type, String title, String content) {
        List<SysUser> admins = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .like(SysUser::getRoles, "ROLE_ADMIN")
                .eq(SysUser::getEnabled, true));
        for (SysUser admin : admins) {
            create(admin.getId(), type, title, content);
        }
    }

    private NotificationDto toDto(Notification n) {
        long ts = n.getCreatedAt() == null ? System.currentTimeMillis() :
                n.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getContent(), Boolean.TRUE.equals(n.getReadFlag()), ts);
    }
}
