package com.example.instantchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register the /ws STOMP endpoint.
     * UserHandshakeHandler assigns a Principal from the userId session attribute.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new UserHandshakeHandler()) // custom Principal assignment
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Configure message broker:
     * /topic  -> broadcast (not heavily used here, but good practice)
     * /queue  -> private queues (our one-to-one messages use this)
     * /app    -> prefix for @MessageMapping methods
     * /user   -> prefix for user-specific destinations
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Register our StompChannelInterceptor on the inbound channel.
     * This intercepts CONNECT frames and extracts the userId header.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompChannelInterceptor());
    }
}
