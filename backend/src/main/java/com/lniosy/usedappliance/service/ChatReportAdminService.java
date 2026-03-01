package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.admin.AdminChatReportDto;
import com.lniosy.usedappliance.entity.ChatMessage;
import com.lniosy.usedappliance.entity.ChatMessageReport;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.ChatMessageMapper;
import com.lniosy.usedappliance.mapper.ChatMessageReportMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatReportAdminService {
    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "CONFIRMED", "REJECTED");
    private final ChatMessageReportMapper chatMessageReportMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    public ChatReportAdminService(ChatMessageReportMapper chatMessageReportMapper,
                                  ChatMessageMapper chatMessageMapper,
                                  SysUserMapper sysUserMapper,
                                  NotificationService notificationService) {
        this.chatMessageReportMapper = chatMessageReportMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.sysUserMapper = sysUserMapper;
        this.notificationService = notificationService;
    }

    public List<AdminChatReportDto> list(String status, String keyword) {
        LambdaQueryWrapper<ChatMessageReport> qw = new LambdaQueryWrapper<ChatMessageReport>().orderByDesc(ChatMessageReport::getId);
        if (status != null && !status.isBlank()) {
            qw.eq(ChatMessageReport::getStatus, status.toUpperCase());
        }
        List<ChatMessageReport> reports = chatMessageReportMapper.selectList(qw);
        if (reports.isEmpty()) {
            return List.of();
        }

        Set<Long> messageIds = reports.stream().map(ChatMessageReport::getMessageId).collect(Collectors.toSet());
        Map<Long, ChatMessage> messageMap = chatMessageMapper.selectBatchIds(messageIds).stream()
                .collect(Collectors.toMap(ChatMessage::getId, m -> m));

        Set<Long> userIds = reports.stream()
                .flatMap(r -> {
                    ChatMessage msg = messageMap.get(r.getMessageId());
                    Long reported = msg == null ? null : msg.getFromUserId();
                    return java.util.stream.Stream.of(r.getReporterId(), r.getReviewedBy(), reported);
                })
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        String kw = keyword == null ? "" : keyword.trim();
        return reports.stream()
                .map(r -> toDto(r, messageMap.get(r.getMessageId()), userMap))
                .filter(d -> kw.isEmpty()
                        || String.valueOf(d.id()).contains(kw)
                        || safe(d.reporterName()).contains(kw)
                        || safe(d.reportedUserName()).contains(kw)
                        || safe(d.reason()).contains(kw)
                        || safe(d.messageContent()).contains(kw))
                .toList();
    }

    public void resolve(Long id, Long adminId, String status, String note) {
        String s = status == null ? "" : status.toUpperCase();
        if (!ALLOWED_STATUS.contains(s) || "PENDING".equals(s)) {
            throw new BizException(400, "状态仅支持 CONFIRMED/REJECTED");
        }
        ChatMessageReport report = chatMessageReportMapper.selectById(id);
        if (report == null) {
            throw new BizException(404, "举报不存在");
        }
        chatMessageReportMapper.update(null, new LambdaUpdateWrapper<ChatMessageReport>()
                .eq(ChatMessageReport::getId, id)
                .set(ChatMessageReport::getStatus, s)
                .set(ChatMessageReport::getReviewedBy, adminId)
                .set(ChatMessageReport::getDecisionNote, note)
                .set(ChatMessageReport::getReviewedAt, LocalDateTime.now()));

        ChatMessage msg = chatMessageMapper.selectById(report.getMessageId());
        if (msg != null) {
            notificationService.create(report.getReporterId(), "REPORT", "举报处理结果",
                    "举报#" + id + " 已处理为: " + s + (note == null ? "" : "，说明: " + note));
            if ("CONFIRMED".equals(s)) {
                notificationService.create(msg.getFromUserId(), "VIOLATION", "违规提醒",
                        "您的消息被判定违规，举报单#" + id + (note == null ? "" : "，说明: " + note));
            }
        }
    }

    private AdminChatReportDto toDto(ChatMessageReport r, ChatMessage msg, Map<Long, SysUser> userMap) {
        Long reportedUserId = msg == null ? 0L : msg.getFromUserId();
        SysUser reporter = userMap.get(r.getReporterId());
        SysUser reported = userMap.get(reportedUserId);
        SysUser reviewer = r.getReviewedBy() == null ? null : userMap.get(r.getReviewedBy());
        long createdAt = r.getCreatedAt() == null ? System.currentTimeMillis() :
                r.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long reviewedAt = r.getReviewedAt() == null ? null :
                r.getReviewedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new AdminChatReportDto(
                r.getId(),
                r.getMessageId(),
                r.getReporterId(),
                reporter == null ? null : reporter.getNickname(),
                reportedUserId,
                reported == null ? null : reported.getNickname(),
                msg == null ? null : msg.getContentType(),
                msg == null ? null : msg.getContent(),
                r.getReason(),
                r.getStatus(),
                r.getReviewedBy(),
                reviewer == null ? null : reviewer.getNickname(),
                r.getDecisionNote(),
                createdAt,
                reviewedAt
        );
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
