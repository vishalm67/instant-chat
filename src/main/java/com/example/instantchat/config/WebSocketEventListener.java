package com.example.instantchat.config;

import com.example.instantchat.dto.UserStatusMessage;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    // Thread-safe set to store currently connected userIds
    private final Set<String> onlineUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Fired when a user connects via WebSocket
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() != null) {
            String userId = accessor.getUser().getName();
            onlineUsers.add(userId);

            // Broadcast to everyone that this user is now online
            messagingTemplate.convertAndSend("/topic/status",
                    new UserStatusMessage(userId, "ONLINE"));

            System.out.println("User connected: " + userId + " | Online users: " + onlineUsers);
        }
    }

    // Fired when a user disconnects (closes tab, logs out, loses internet)
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() != null) {
            String userId = accessor.getUser().getName();
            onlineUsers.remove(userId);

            // Broadcast to everyone that this user is now offline
            messagingTemplate.convertAndSend("/topic/status",
                    new UserStatusMessage(userId, "OFFLINE"));

            System.out.println("User disconnected: " + userId + " | Online users: " + onlineUsers);
        }
    }

    // Returns the set of currently online userIds
    public Set<String> getOnlineUsers() {
        return Collections.unmodifiableSet(onlineUsers);
    }
}