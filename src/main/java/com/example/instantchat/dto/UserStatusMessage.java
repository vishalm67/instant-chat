package com.example.instantchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Sent to all clients when a user comes online or goes offline
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusMessage {
    private String userId;
    private String status; // "ONLINE" or "OFFLINE"
}