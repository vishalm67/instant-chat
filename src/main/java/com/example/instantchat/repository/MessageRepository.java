package com.example.instantchat.repository;

import com.example.instantchat.model.Message;
import com.example.instantchat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Fetch conversation between two users, ordered by time.
     * Returns all messages where (sender=A, receiver=B) OR (sender=B, receiver=A).
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.timestamp ASC")
    List<Message> findConversation(@Param("user1") User user1, @Param("user2") User user2);
    // Count unread messages sent by a specific sender to a specific receiver
    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
    int countUnreadMessages(@Param("sender") User sender, @Param("receiver") User receiver);

    // Mark all messages from sender to receiver as read
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
    void markMessagesAsRead(@Param("sender") User sender, @Param("receiver") User receiver);
}
