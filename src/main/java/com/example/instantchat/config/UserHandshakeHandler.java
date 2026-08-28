package com.example.instantchat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Custom handshake handler.
 * When a WebSocket connection is established, this is called.
 * We read the "userId" attribute (set by the channel interceptor) and
 * create a UserIdPrincipal so Spring can route /user/{id}/queue/messages.
 */
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        // The userId was placed in attributes by StompChannelInterceptor
        String userId = (String) attributes.get("userId");
        if (userId != null && !userId.isEmpty()) {
            return new UserIdPrincipal(userId);
        }
        // Fallback: assign a random name (shouldn't happen in normal flow)
        return new UserIdPrincipal("anonymous-" + System.currentTimeMillis());
    }
}
