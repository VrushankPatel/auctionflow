package com.auctionflow.notifications;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ConnectInterceptor connectInterceptor;
    private final SubscriptionInterceptor subscriptionInterceptor;

    public WebSocketConfig(ConnectInterceptor connectInterceptor, SubscriptionInterceptor subscriptionInterceptor) {
        this.connectInterceptor = connectInterceptor;
        this.subscriptionInterceptor = subscriptionInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for topics and queues
        // For production scalability, replace with Redis Pub/Sub integration
        // Redis is configured for presence tracking and pub/sub support
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 20000}); // Client heartbeat 10s, server 20s

        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure CORS as needed
                .withSockJS(); // Enable SockJS fallback
                // .addInterceptors(new JwtHandshakeInterceptor()); // Commented out due to API changes
    }

    // @Override
    // public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    //     registration.setMessageSizeLimit(64 * 1024) // 64KB message size
    //             .setSendBufferSizeLimit(512 * 1024) // 512KB send buffer
    //             .setSendTimeLimit(20000); // 20 seconds send time limit
    // }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(connectInterceptor, subscriptionInterceptor);
    }
}