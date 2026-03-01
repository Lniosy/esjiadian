package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.chat.ChatMessageDto;
import com.lniosy.usedappliance.dto.chat.ChatMessageRequest;
import com.lniosy.usedappliance.dto.chat.ChatReportRequest;
import com.lniosy.usedappliance.dto.chat.ChatReadRequest;
import com.lniosy.usedappliance.service.ChatService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<String>> conversations() {
        return ApiResponse.ok(chatService.conversations(SecurityUtils.currentUserId()));
    }

    @GetMapping("/messages")
    public ApiResponse<List<ChatMessageDto>> messages(@RequestParam String conversationId) {
        return ApiResponse.ok(chatService.list(conversationId));
    }

    @PostMapping("/messages")
    public ApiResponse<ChatMessageDto> send(@RequestBody @Valid ChatMessageRequest req) {
        ChatMessageDto msg = chatService.send(SecurityUtils.currentUserId(), req);
        messagingTemplate.convertAndSendToUser(String.valueOf(req.toUserId()), "/queue/chat.receive", msg);
        return ApiResponse.ok(msg);
    }

    @PostMapping("/messages/{id}/report")
    public ApiResponse<Void> report(@PathVariable Long id, @RequestBody @Valid ChatReportRequest req) {
        chatService.reportMessage(SecurityUtils.currentUserId(), id, req.reason());
        return ApiResponse.ok("举报已提交", null);
    }

    @PostMapping("/read")
    public ApiResponse<Integer> read(@RequestBody @Valid ChatReadRequest req) {
        int affected = chatService.markConversationRead(SecurityUtils.currentUserId(), req.conversationId());
        return ApiResponse.ok(affected);
    }

    @MessageMapping("/chat.send")
    public void sendWs(@Payload ChatMessageRequest req, Principal principal) {
        if (principal == null) {
            return;
        }
        Long fromUserId = Long.valueOf(principal.getName());
        ChatMessageDto msg = chatService.send(fromUserId, req);
        messagingTemplate.convertAndSendToUser(String.valueOf(req.toUserId()), "/queue/chat.receive", msg);
    }

    @MessageMapping("/chat.read")
    public void readWs(@Payload ChatReadRequest req, Principal principal) {
        if (principal == null) {
            return;
        }
        Long userId = Long.valueOf(principal.getName());
        int affected = chatService.markConversationRead(userId, req.conversationId());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/chat.read",
                ApiResponse.ok(affected)
        );
    }
}
