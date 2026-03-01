package com.lniosy.usedappliance.config;

import com.lniosy.usedappliance.security.AuthUser;
import com.lniosy.usedappliance.security.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token != null && !token.isBlank()) {
                try {
                    AuthUser authUser = jwtTokenProvider.toAuthUser(token);
                    Principal principal = () -> String.valueOf(authUser.userId());
                    accessor.setUser(principal);
                } catch (Exception ignored) {
                    // ignore invalid token, websocket connect will still be established without user principal
                }
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        String auth = authHeaders.get(0);
        if (auth == null) {
            return null;
        }
        if (auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return auth;
    }
}
