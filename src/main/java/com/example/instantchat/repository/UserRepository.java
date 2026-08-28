package com.example.instantchat.repository;

import com.example.instantchat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email (used during login)
    Optional<User> findByEmail(String email);

    // Check if username is already taken
    boolean existsByUsername(String username);

    // Check if email is already registered
    boolean existsByEmail(String email);
}
