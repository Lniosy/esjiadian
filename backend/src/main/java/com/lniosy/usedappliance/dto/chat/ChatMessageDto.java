package com.lniosy.usedappliance.dto.chat;

public record ChatMessageDto(
        Long id,
        String conversationId,
        Long fromUserId,
        Long toUserId,
        String contentType,
        String content,
        long sentAt,
        boolean read
) {
}
