package com.example.instantchat.controller;

import com.example.instantchat.dto.LoginRequest;
import com.example.instantchat.dto.RegisterRequest;
import com.example.instantchat.dto.UserResponse;
import com.example.instantchat.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.instantchat.config.WebSocketEventListener;
import java.util.Set;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // Allow all origins for development
public class UserController {

    private final UserService userService;
    private final WebSocketEventListener webSocketEventListener;
    public UserController(UserService userService, WebSocketEventListener webSocketEventListener) {
        this.userService = userService;
        this.webSocketEventListener = webSocketEventListener;
    }
 //Shows the user is online or offline
    @GetMapping("/online")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(webSocketEventListener.getOnlineUsers());
    }


    /**
     * POST /api/users/register
     * Register a new user.
     * Returns 201 CREATED with user info on success.
     * Returns 409 CONFLICT if username/email already exists.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserResponse user = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (RuntimeException e) {
            // Return user-friendly error message (409 Conflict)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * POST /api/users/login
     * Authenticate user.
     * Returns 200 OK with user info on success.
     * Returns 401 UNAUTHORIZED on invalid credentials.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            UserResponse user = userService.login(request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/users
     * Return all registered users.
     * Frontend uses this to populate the sidebar user list.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
