package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.chat.ChatMessageDto;
import com.lniosy.usedappliance.dto.chat.ChatMessageRequest;
import com.lniosy.usedappliance.entity.ChatMessageReport;
import com.lniosy.usedappliance.entity.ChatMessage;
import com.lniosy.usedappliance.mapper.ChatMessageMapper;
import com.lniosy.usedappliance.mapper.ChatMessageReportMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ChatService {
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageReportMapper chatMessageReportMapper;

    public ChatService(ChatMessageMapper chatMessageMapper, ChatMessageReportMapper chatMessageReportMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.chatMessageReportMapper = chatMessageReportMapper;
    }

    public ChatMessageDto send(Long fromUserId, ChatMessageRequest req) {
        ChatMessage m = new ChatMessage();
        m.setConversationId(conversationId(fromUserId, req.toUserId()));
        m.setFromUserId(fromUserId);
        m.setToUserId(req.toUserId());
        m.setContentType(req.contentType());
        m.setContent(req.content());
        m.setReadFlag(false);
        chatMessageMapper.insert(m);
        return toDto(m);
    }

    public List<ChatMessageDto> list(String conversationId, Long userId) {
        // 校验用户是否为会话参与者
        String[] ids = conversationId.split("_");
        if (ids.length != 2 || (!ids[0].equals(String.valueOf(userId)) && !ids[1].equals(String.valueOf(userId)))) {
            throw new BizException(403, "无权查看该会话消息");
        }
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByAsc(ChatMessage::getId))
                .stream().map(this::toDto).toList();
    }

    public List<String> conversations(Long userId) {
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .and(w -> w.eq(ChatMessage::getFromUserId, userId).or().eq(ChatMessage::getToUserId, userId))
                        .orderByDesc(ChatMessage::getId))
                .stream().map(ChatMessage::getConversationId).distinct().toList();
    }

    public int markConversationRead(Long userId, String conversationId) {
        return chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getReadFlag, false)
                .set(ChatMessage::getReadFlag, true));
    }

    public int cleanupExpiredMessages(int retentionDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(retentionDays);
        return chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .lt(ChatMessage::getSentAt, deadline));
    }

    public void reportMessage(Long reporterId, Long messageId, String reason) {
        ChatMessage msg = chatMessageMapper.selectById(messageId);
        if (msg == null) {
            throw new BizException(404, "消息不存在");
        }
        boolean participant = reporterId.equals(msg.getFromUserId()) || reporterId.equals(msg.getToUserId());
        if (!participant) {
            throw new BizException(403, "无权举报该消息");
        }
        ChatMessageReport report = new ChatMessageReport();
        report.setMessageId(messageId);
        report.setReporterId(reporterId);
        report.setReason(reason);
        report.setStatus("PENDING");
        chatMessageReportMapper.insert(report);
    }

    private String conversationId(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + "_" + max;
    }

    private ChatMessageDto toDto(ChatMessage m) {
        long sentAt = m.getSentAt() == null ? System.currentTimeMillis() :
                m.getSentAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new ChatMessageDto(m.getId(), m.getConversationId(), m.getFromUserId(), m.getToUserId(),
                m.getContentType(), m.getContent(), sentAt, Boolean.TRUE.equals(m.getReadFlag()));
    }
}
