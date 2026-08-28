package com.example.instantchat.service;

import com.example.instantchat.dto.MessageRequest;
import com.example.instantchat.dto.MessageResponse;
import com.example.instantchat.model.Message;
import com.example.instantchat.model.User;
import com.example.instantchat.repository.MessageRepository;
import com.example.instantchat.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Save a new message to the database and return it as a response DTO.
     * Called from the WebSocket controller after receiving a message.
     */
    public MessageResponse saveMessage(MessageRequest request) {
        // Load sender and receiver from DB
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // Create and save Message entity
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setMessage(request.getMessage());
        // timestamp is set automatically by @PrePersist in Message entity

        Message saved = messageRepository.save(message);
        return toMessageResponse(saved);
    }

    /**
     * Retrieve conversation history between two users.
     * Called when a user clicks on another user in the sidebar.
     */
    public List<MessageResponse> getConversation(Long userId, Long otherUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Other user not found"));

        List<Message> messages = messageRepository.findConversation(user, otherUser);

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Message entity to MessageResponse DTO.
     */
    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getReceiver().getId(),
                message.getReceiver().getUsername(),
                message.getMessage(),
                message.getTimestamp(),
                message.isRead()
        );
    }
    /**
     * Returns unread message count per sender for a given user.
     * Example: { "1": 3, "4": 1 } means user 1 sent 3 unread, user 4 sent 1 unread.
     */
    public Map<Long, Integer> getUnreadCounts(Long userId) {
        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<User> allUsers = userRepository.findAll();
        Map<Long, Integer> unreadCounts = new HashMap<>();

        for (User sender : allUsers) {
            if (!sender.getId().equals(userId)) {
                int count = messageRepository.countUnreadMessages(sender, receiver);
                if (count > 0) {
                    unreadCounts.put(sender.getId(), count);
                }
            }
        }

        return unreadCounts;
    }

    /**
     * Mark all messages from a specific sender as read.
     * Called when the user opens a conversation.
     */
    @Transactional
    public void markAsRead(Long receiverId, Long senderId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        messageRepository.markMessagesAsRead(sender, receiver);
    }
}
