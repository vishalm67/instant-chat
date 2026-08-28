package com.example.instantchat.config;

import java.security.Principal;

/**
 * A simple Principal implementation that identifies a WebSocket user by their numeric userId.
 * This allows Spring to route messages to /user/{userId}/queue/messages correctly.
 */
public class UserIdPrincipal implements Principal {

    private final String userId;

    public UserIdPrincipal(String userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return userId; // Spring uses this as the "username" for user-destination routing
    }
}
