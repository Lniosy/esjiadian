package com.lniosy.usedappliance.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageRequest(@NotNull Long toUserId, @NotBlank String contentType, @NotBlank String content) {
}
