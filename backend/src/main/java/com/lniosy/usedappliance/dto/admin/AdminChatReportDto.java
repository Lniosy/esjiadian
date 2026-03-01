package com.lniosy.usedappliance.dto.admin;

public record AdminChatReportDto(
        Long id,
        Long messageId,
        Long reporterId,
        String reporterName,
        Long reportedUserId,
        String reportedUserName,
        String messageType,
        String messageContent,
        String reason,
        String status,
        Long reviewedBy,
        String reviewedByName,
        String decisionNote,
        long createdAt,
        Long reviewedAt
) {
}
