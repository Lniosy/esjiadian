package com.lniosy.usedappliance.config;

import com.lniosy.usedappliance.service.ChatService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChatRetentionJob {
    private final ChatService chatService;

    public ChatRetentionJob(ChatService chatService) {
        this.chatService = chatService;
    }

    @Scheduled(cron = "${app.chat.retention-cron:0 10 3 * * *}")
    public void cleanupExpired() {
        chatService.cleanupExpiredMessages(90);
    }
}
