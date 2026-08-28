package com.example.instantchat.controller;

import com.example.instantchat.dto.MessageRequest;
import com.example.instantchat.dto.MessageResponse;
import com.example.instantchat.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class MessageController {

    private final MessageService messageService;

    // SimpMessagingTemplate allows us to programmatically send messages to specific users
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * WebSocket endpoint: /app/chat.sendMessage
     *
     * Flow:
     * 1. JavaScript sends message to /app/chat.sendMessage via STOMP
     * 2. This method receives it
     * 3. Saves it to MySQL
     * 4. Sends to receiver via /user/{receiverId}/queue/messages
     * 5. Also echoes back to sender via /user/{senderId}/queue/messages
     *    (so sender sees their own message in real time)
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageRequest request) {
        // Save message to database
        MessageResponse savedMessage = messageService.saveMessage(request);

        // Send message to the RECEIVER's private queue
        // The receiver's JS is subscribed to /user/queue/messages
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getReceiverId()),
                "/queue/messages",
                savedMessage
        );

        // Also send back to the SENDER so they see their own message immediately
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getSenderId()),
                "/queue/messages",
                savedMessage
        );
    }

    /**
     * GET /api/messages/{otherUserId}?userId={currentUserId}
     * Fetch chat history between the logged-in user and another user.
     * Called when a user clicks on someone in the sidebar.
     */
    @GetMapping("/api/messages/{otherUserId}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long otherUserId,
            @RequestParam Long userId) {
        List<MessageResponse> messages = messageService.getConversation(userId, otherUserId);
        return ResponseEntity.ok(messages);
    }
    /**
     * GET /api/messages/unread?userId={currentUserId}
     * Returns unread message counts per sender.
     * Example response: { "2": 3, "5": 1 }
     */
    @GetMapping("/api/messages/unread")
    public ResponseEntity<Map<Long, Integer>> getUnreadCounts(@RequestParam Long userId) {
        return ResponseEntity.ok(messageService.getUnreadCounts(userId));
    }

    /**
     * POST /api/messages/read?receiverId={currentUserId}&senderId={otherUserId}
     * Marks all messages from sender to receiver as read.
     * Called when user opens a conversation.
     */
    @PostMapping("/api/messages/read")
    public ResponseEntity<?> markAsRead(
            @RequestParam Long receiverId,
            @RequestParam Long senderId) {
        messageService.markAsRead(receiverId, senderId);
        return ResponseEntity.ok().build();
    }
}
