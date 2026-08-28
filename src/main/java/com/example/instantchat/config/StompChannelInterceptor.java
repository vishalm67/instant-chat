package com.example.instantchat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.Map;

/**
 * Intercepts STOMP CONNECT frames.
 * Extracts the "userId" header that the JavaScript sends in connectHeaders
 * and stores it in the WebSocket session attributes.
 * The UserHandshakeHandler then reads it to assign a Principal.
 */
public class StompChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Only process CONNECT frames
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = accessor.getFirstNativeHeader("userId");
            if (userId != null && !userId.isEmpty()) {
                // Store userId in session attributes for UserHandshakeHandler
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    sessionAttributes.put("userId", userId);
                }
                // Also set as the Principal directly on the accessor
                accessor.setUser(new UserIdPrincipal(userId));
            }
        }

        return message;
    }
}
