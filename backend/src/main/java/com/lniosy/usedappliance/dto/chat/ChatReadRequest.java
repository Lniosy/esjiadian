package com.lniosy.usedappliance.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatReadRequest(
        @NotBlank(message = "会话ID不能为空") String conversationId
) {
}
