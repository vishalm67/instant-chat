package com.example.instantchat.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "users")
@Data                   // Lombok: generates getters, setters, equals, hashCode, toString
@NoArgsConstructor      // Lombok: generates no-arg constructor
@AllArgsConstructor     // Lombok: generates all-arg constructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)// auto-increment primary key
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // stored as BCrypt hash, never plain text

    // A user can send many messages
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> sentMessages;

    // A user can receive many messages
    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> receivedMessages;
}
