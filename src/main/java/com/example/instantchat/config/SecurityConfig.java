package com.example.instantchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Allow all HTTP requests without authentication.
     * We handle our own simple session-based auth via userId stored in localStorage.
     * JWT / full Spring Security can be added as a future enhancement.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())           // disable CSRF for simplicity
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()            // allow all requests
            );
        return http.build();
    }

    /**
     * BCryptPasswordEncoder bean - used for hashing passwords before saving to DB.
     * BCrypt automatically salts passwords, making them secure.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
